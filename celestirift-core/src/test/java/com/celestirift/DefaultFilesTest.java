package com.celestirift;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Every bundled skill file is installed on a fresh server, and nothing in the list is missing. */
class DefaultFilesTest {

    @Test
    void defaultSkillFilesMatchResources() throws IOException {
        Path root = Path.of("src/main/resources");
        Set<String> onDisk;
        try (Stream<Path> files = Files.walk(root.resolve("skills"))) {
            onDisk = files.filter(Files::isRegularFile)
                    .map(p -> root.relativize(p).toString().replace(File.separatorChar, '/'))
                    .collect(Collectors.toCollection(TreeSet::new));
        }
        assertEquals(onDisk, new TreeSet<>(CelestiRift.DEFAULT_SKILL_FILES));
    }

    @Test
    void defaultGemFilesMatchResources() throws IOException {
        Path root = Path.of("src/main/resources");
        Set<String> onDisk;
        try (Stream<Path> files = Files.walk(root.resolve("gems"))) {
            onDisk = files.filter(Files::isRegularFile)
                    .map(p -> root.relativize(p).toString().replace(File.separatorChar, '/'))
                    .collect(Collectors.toCollection(TreeSet::new));
        }
        assertEquals(onDisk, new TreeSet<>(CelestiRift.DEFAULT_GEM_FILES));
    }

    @Test
    void defaultFusionFilesMatchResources() throws IOException {
        Path root = Path.of("src/main/resources");
        Set<String> onDisk;
        try (Stream<Path> files = Files.walk(root.resolve("fusions"))) {
            onDisk = files.filter(Files::isRegularFile)
                    .map(p -> root.relativize(p).toString().replace(File.separatorChar, '/'))
                    .collect(Collectors.toCollection(TreeSet::new));
        }
        assertEquals(onDisk, new TreeSet<>(CelestiRift.DEFAULT_FUSION_FILES));
    }
}
