package com.celestirift.skill.mechanic.impl;

import com.celestirift.CelestiRift;
import com.celestirift.api.skill.ConfigMap;
import com.celestirift.api.skill.SkillMetadata;
import com.celestirift.api.skill.mechanic.TargetedLocationMechanic;
import com.celestirift.integration.Orion;
import com.celestirift.item.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;
import com.celestirift.util.Registries;
import com.celestirift.vfx.DisplayFx;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Locale;

/**
 * {@code display} - a 3D object for a moment: a block, an item (any resource-pack model)
 * or text, that can grow, spin, rise and follow the caster.
 * <pre>
 * - display: { type: item, material: IRON_SWORD, scale: 2, tilt: 180, spin: 12, rise: 1.5, duration: 30 }
 *   target: target                                   # a sword spinning above the target
 * - display: { type: block, material: PACKED_ICE, scale: 0.2, endscale: 1.6, duration: 12, onend: IceBurst }
 *   target: { nearby: 6 }                            # ice spikes growing under enemies
 * - display: { type: text, text: "<red><bold>!", rise: 1, duration: 20 }
 * - display: { type: item, item: demo:fire_orb, spin: 20, duration: 30 }   # an OrionRP custom item
 * - display: { type: sprite, frames: "boom_1,boom_2,boom_3,boom_4", frametime: 2, scale: 4, duration: 8 }
 * - display: { type: sprite, glyph: magic_circle, tilt: -90, scale: 6, spin: 6, duration: 40 }  # flat on the ground
 * </pre>
 * {@code sprite} shows OrionRP glyphs ({@code textures/glyph/<name>.png} in any pack source) as a
 * flat, full-bright image facing the player; several {@code frames} play as an animation (a frame
 * every {@code frametime} ticks, looping). Needs OrionRP; a missing glyph shows nothing.
 * {@code onend} casts a skill at the display's last position. Effects are never saved
 * to disk and are removed on reload/shutdown.
 */
public final class DisplayMechanic implements TargetedLocationMechanic {

    private final CelestiRift plugin;
    private final String type;
    private final String material;
    private final String model;
    private final String text;
    private final float scale;
    private final float endScale;
    private final float rise;
    private final float spin;
    private final float tilt;
    private final float yOffset;
    private final int duration;
    private final boolean follow;
    private final boolean glow;
    private final String glowColor;
    private final String billboard;
    private final String onEnd;
    private final List<String> frames;
    private final int frameTime;
    /** Missing glyphs already reported, so a skill cast every second doesn't flood the log. */
    private static final java.util.Set<String> MISSING = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public DisplayMechanic(CelestiRift plugin, ConfigMap cfg) {
        this.plugin = plugin;
        this.type = cfg.getChoice("block", List.of("block", "item", "text", "sprite"), "type", "t");
        String frameList = cfg.getString("", "frames", "glyph", "glyphs", "sprite");
        this.frames = java.util.Arrays.stream(frameList.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
        this.frameTime = Math.max(1, cfg.getInt(2, "frametime", "frame"));
        this.material = cfg.getString("", "material", "m", "block", "item");
        this.model = cfg.getString("", "model");
        this.text = cfg.getString("", "text");
        this.scale = (float) cfg.getDouble(1, "scale", "size");
        this.endScale = (float) cfg.getDouble(Double.NaN, "endscale", "grow");
        this.rise = (float) cfg.getDouble(0, "rise");
        this.spin = (float) cfg.getDouble(0, "spin");
        this.tilt = (float) cfg.getDouble(0, "tilt", "pitch");
        this.yOffset = (float) cfg.getDouble(0, "y", "offsety");
        this.duration = Math.max(1, cfg.getInt(40, "duration", "d", "ticks"));
        this.follow = cfg.getBoolean(false, "follow");
        this.glow = cfg.getBoolean(false, "glow");
        this.glowColor = cfg.getString("", "glowcolor");
        this.billboard = cfg.getChoice(type.equals("text") || type.equals("sprite") ? "center" : "fixed",
                List.of("fixed", "center", "vertical", "horizontal"), "billboard", "face");
        this.onEnd = cfg.getString("", "onend");
    }

    @Override
    public void cast(SkillMetadata meta, Location target) {
        Location at = target.clone().add(0, yOffset, 0);
        at.setYaw(0);
        at.setPitch(0);
        float yaw = meta.caster().getLocation().getYaw();
        float end = Float.isNaN(endScale) ? scale : endScale;
        boolean centered = type.equals("block");

        Display d = switch (type) {
            case "item" -> DisplayFx.spawn(at, ItemDisplay.class, e -> {
                // "ns:item" (not minecraft:) is an OrionRP custom item, shown with its own model.
                ItemStack custom = material.contains(":") && !material.startsWith("minecraft:")
                        ? Orion.item(material, 1) : null;
                if (custom != null) {
                    e.setItemStack(custom);
                    return;
                }
                Material m = Registries.itemMaterial(material.isBlank() ? "IRON_SWORD" : material)
                        .orElse(Material.IRON_SWORD);
                ItemBuilder b = ItemBuilder.of(m);
                if (!model.isBlank()) {
                    b.model(model);
                }
                e.setItemStack(b.build());
            });
            case "sprite" -> DisplayFx.spawn(at, TextDisplay.class, e -> {
                e.text(frame(0));
                e.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
                e.setShadowed(false);
                e.setLineWidth(4096);
                e.setBrightness(new Display.Brightness(15, 15));
            });
            case "text" -> DisplayFx.spawn(at, TextDisplay.class, e -> {
                e.text(ItemBuilder.mm(com.celestirift.skill.Placeholders.apply(text, meta, null)));
                e.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
                e.setShadowed(true);
            });
            default -> DisplayFx.spawn(at, BlockDisplay.class, e -> {
                Material m = Material.matchMaterial(material.isBlank() ? "ICE" : material.toUpperCase(Locale.ROOT));
                e.setBlock((m != null && m.isBlock() ? m : Material.ICE).createBlockData());
            });
        };
        d.setBillboard(Display.Billboard.valueOf(billboard.toUpperCase(Locale.ROOT)));
        d.setTeleportDuration(2);
        d.setTransformation(DisplayFx.transform(scale, yaw, tilt, 0, centered));
        if (glow) {
            d.setGlowing(true);
            if (!glowColor.isBlank()) {
                try {
                    d.setGlowColorOverride(Color.fromRGB(Integer.parseInt(glowColor.replace("#", ""), 16)));
                } catch (NumberFormatException ignored) {
                    // keep the default glow colour
                }
            }
        }

        new BukkitRunnable() {
            int t;

            @Override
            public void run() {
                if (!d.isValid()) {
                    cancel();
                    return;
                }
                t += 2;
                float p = Math.min(1, (float) t / duration);
                if (follow && meta.caster().isValid()) {
                    d.teleport(meta.caster().getLocation().add(0, yOffset, 0));
                }
                float s = scale + (end - scale) * p;
                DisplayFx.animate(d, DisplayFx.transform(s, yaw + spin * t, tilt, rise * p, centered), 2);
                if (d instanceof TextDisplay sprite && type.equals("sprite") && frames.size() > 1
                        && t % frameTime < 2) {
                    sprite.text(frame(t / frameTime));
                }
                if (t >= duration) {
                    cancel();
                    Location last = d.getLocation().add(0, rise, 0);
                    DisplayFx.remove(d);
                    if (!onEnd.isBlank()) {
                        plugin.skills().cast(onEnd, new SkillMetadata(meta.caster(), meta.trigger(), last,
                                meta.power()).rangeMultiplier(meta.rangeMultiplier()));
                    }
                }
            }
        }.runTaskTimer(plugin, 1L, 2L);
    }

    /** Sprite frame {@code index} (looping) as an OrionRP glyph, or nothing if it is missing. */
    private Component frame(int index) {
        if (frames.isEmpty()) {
            return Component.empty();
        }
        String name = frames.get(Math.floorMod(index, frames.size()));
        Component glyph = Orion.glyph(name);
        if (glyph == null) {
            if (MISSING.add(name)) {
                plugin.getLogger().warning("display sprite: no OrionRP glyph '" + name + "' ("
                        + (Orion.available() ? "add textures/glyph/" + name + ".png and /orp rebuild" : "OrionRP is not installed") + ")");
            }
            return Component.empty();
        }
        return glyph;
    }
}
