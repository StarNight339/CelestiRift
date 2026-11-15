package com.celestirift.skill.load;

import com.celestirift.validate.Diagnostics;
import com.celestirift.validate.LineLocator;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Locates content files: a main file (e.g. {@code gems.yml}) plus every YAML file in a
 * matching folder (e.g. {@code gems/**.yml}), so large servers can split content up.
 */
public final class ContentFiles {

    /** A parsed content file. */
    public record File(String path, YamlConfiguration yaml, LineLocator lines) {}

    private ContentFiles() {}

    /**
     * @param mainFile bundled default copied on first run (e.g. {@code gems.yml})
     * @param folder   extra folder scanned recursively (e.g. {@code gems})
     */
    public static List<File> load(Plugin plugin, String mainFile, String folder, Diagnostics diag) {
        Path root = plugin.getDataFolder().toPath();
        Path main = root.resolve(mainFile);
        if (!Files.exists(main) && plugin.getResource(mainFile) != null) {
            plugin.saveResource(mainFile, false);
        }
        List<Path> paths = new ArrayList<>();
        if (Files.exists(main)) {
            paths.add(main);
        }
        paths.addAll(SkillLoader.yamlFiles(root.resolve(folder)));

        List<File> out = new ArrayList<>();
        for (Path p : paths) {
            String rel = root.relativize(p).toString().replace('\\', '/');
            YamlConfiguration yaml = SkillLoader.readYaml(p, rel, diag);
            if (yaml != null) {
                out.add(new File(rel, yaml, new LineLocator(p)));
            }
        }
        return out;
    }
}
