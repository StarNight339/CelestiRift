package com.celestirift.pack;

import com.celestirift.CelestiRift;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

/**
 * The CelestiRift resource pack: extracts the bundled zip, decides which item
 * models/tooltip styles to use, and sends the pack to players on join.
 *
 * <p>Models are only applied while {@code resource-pack.enabled} is true, so servers
 * without the pack never show missing textures. An explicit {@code model:} on a gem
 * or skill is always applied (the author asked for it).
 */
public final class ResourcePackService implements Listener {

    public static final String BUNDLED = "pack/CelestiRift-pack.zip";

    private final CelestiRift plugin;
    private boolean enabled;
    private String url = "";
    private String hash = "";
    private boolean required;
    private String prompt = "";
    private @Nullable String bundledHash;
    private boolean orionrp;

    public ResourcePackService(CelestiRift plugin) {
        this.plugin = plugin;
    }

    /** Extract the bundled pack (always refreshed — it's generated) and read config. */
    public void reload() {
        bundledHash = extractBundled();
        var cfg = plugin.getConfig();
        enabled = cfg.getBoolean("resource-pack.enabled", false);
        url = cfg.getString("resource-pack.send.url", "").trim();
        hash = cfg.getString("resource-pack.send.hash", "").trim();
        required = cfg.getBoolean("resource-pack.send.required", false);
        prompt = cfg.getString("resource-pack.send.prompt", "");

        boolean orionrpInstalled = plugin.getServer().getPluginManager().isPluginEnabled("OrionRP");
        boolean useOrionrp = cfg.getBoolean("resource-pack.use-orionrp", true);
        if (orionrpInstalled && useOrionrp && bundledHash != null) {
            orionrp = OrionRPHook.register(plugin, bundledFile());
        } else {
            if (orionrp && orionrpInstalled) {
                OrionRPHook.unregister(); // use-orionrp was turned off by a reload
            }
            orionrp = false;
        }
        if (orionrp) {
            // OrionRP merges and sends our pack: no own sending, and models are safe to show
            // whenever OrionRP actually delivers the pack to players.
            if (!enabled && OrionRPHook.sendsPack()) {
                enabled = true;
            }
            plugin.getLogger().info("Resource pack is merged and sent by OrionRP"
                    + (enabled ? "." : " - but OrionRP is not sending it, so models stay disabled."));
            return;
        }
        if (enabled && url.isEmpty()) {
            plugin.getLogger().info("Resource pack models are enabled but no send.url is set — "
                    + "players must already have the pack (e.g. merged into your server pack).");
        }
        if (!url.isEmpty() && !url.startsWith("https://") && !url.startsWith("http://")) {
            plugin.getLogger().warning("resource-pack.send.url must be a direct http(s) link to a .zip");
        }
    }

    public boolean enabled() { return enabled; }

    /** True when OrionRP merges and sends the pack (CelestiRift doesn't send it itself). */
    public boolean managedByOrionRP() { return orionrp; }

    /** SHA-1 of the bundled pack zip, or null if it couldn't be read. */
    public @Nullable String bundledHash() { return bundledHash; }

    public Path bundledFile() { return plugin.getDataFolder().toPath().resolve(BUNDLED); }

    public String url() { return url; }

    /** The hash that will be sent: configured, else the bundled pack's. */
    public String effectiveHash() {
        return !hash.isEmpty() ? hash : (bundledHash == null ? "" : bundledHash);
    }

    // ------------------------------------------------------------------ item looks

    /** Default gem model for a tier, or null when the pack is disabled/unset. */
    public @Nullable String gemModel(int tier, boolean passive) {
        if (!enabled) {
            return null;
        }
        if (!passive) {
            String perTier = plugin.getConfig().getString("resource-pack.tier-models." + tier, "");
            if (!perTier.isBlank()) {
                return perTier;
            }
        }
        String key = passive ? "rune" : "gem";
        String v = plugin.getConfig().getString("resource-pack.models." + key, "");
        return v.isBlank() ? null : v.replace("{tier}", String.valueOf(tier));
    }

    /**
     * Look for one gem while the pack is on: resource-pack.gem-models.<GemId> (any case),
     * else the tier default. Unlike a gem's own "model:", this is skipped when the pack is
     * off, so players without the pack never see a missing texture.
     */
    public @Nullable String gemModel(String gemId, int tier, boolean passive) {
        if (!enabled) {
            return null;
        }
        var section = plugin.getConfig().getConfigurationSection("resource-pack.gem-models");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                if (key.equalsIgnoreCase(gemId)) {
                    String v = section.getString(key, "");
                    if (!v.isBlank()) {
                        return v;
                    }
                }
            }
        }
        return gemModel(tier, passive);
    }

    public @Nullable String model(String key) {
        if (!enabled) {
            return null;
        }
        String v = plugin.getConfig().getString("resource-pack.models." + key, "");
        return v.isBlank() ? null : v;
    }

    public @Nullable String tooltipStyle(int tier) {
        if (!enabled) {
            return null;
        }
        ConfigurationSection s = plugin.getConfig().getConfigurationSection("resource-pack.tooltip-styles");
        if (s == null) {
            return null;
        }
        String v = s.getString(String.valueOf(tier), "");
        return v.isBlank() ? null : v;
    }

    // ------------------------------------------------------------------ sending

    /** Send the pack to a player. Returns false if no URL is configured. */
    public boolean send(Player player) {
        if (url.isEmpty()) {
            return false;
        }
        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid resource-pack.send.url: " + url);
            return false;
        }
        // Stable id per URL so clients replace (not stack) the pack after updates.
        UUID id = UUID.nameUUIDFromBytes(("celestirift:" + url).getBytes(StandardCharsets.UTF_8));
        ResourcePackInfo info = ResourcePackInfo.resourcePackInfo(id, uri, effectiveHash());
        ResourcePackRequest.Builder request = ResourcePackRequest.resourcePackRequest()
                .packs(info)
                .replace(false)
                .required(required);
        if (!prompt.isBlank()) {
            request.prompt(MiniMessage.miniMessage().deserialize(prompt));
        }
        player.sendResourcePacks(request.build());
        return true;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (enabled && !orionrp) {
            send(event.getPlayer());
        }
    }

    @EventHandler
    public void onStatus(PlayerResourcePackStatusEvent event) {
        if (orionrp) {
            return; // OrionRP sends (and reports on) the merged pack
        }
        switch (event.getStatus()) {
            case FAILED_DOWNLOAD, INVALID_URL, FAILED_RELOAD, DISCARDED -> plugin.getLogger().warning(
                    event.getPlayer().getName() + " could not load the CelestiRift resource pack ("
                            + event.getStatus() + "). Check resource-pack.send.url/hash.");
            default -> { }
        }
    }

    // ------------------------------------------------------------------ bundled zip

    private @Nullable String extractBundled() {
        Path target = bundledFile();
        try (InputStream in = plugin.getResource(BUNDLED)) {
            if (in == null) {
                return null;
            }
            Files.createDirectories(target.getParent());
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            return sha1(Files.readAllBytes(target));
        } catch (IOException e) {
            plugin.getLogger().warning("Could not extract the bundled resource pack: " + e.getMessage());
            return null;
        }
    }

    private static String sha1(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
