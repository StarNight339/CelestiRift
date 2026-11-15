package com.celestirift.validate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Finds approximate line numbers in a YAML file (Bukkit's YAML API doesn't keep
 * them): the line of a top-level key, and the first line inside that block that
 * contains some text (a mechanic name, an attribute...).
 */
public final class LineLocator {

    private final List<String> lines;

    public LineLocator(Path file) {
        List<String> read;
        try {
            read = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            read = List.of();
        }
        this.lines = read;
    }

    /** 1-based line of a top-level key, or 0. */
    public int key(String key) {
        Pattern p = Pattern.compile("^[\"']?" + Pattern.quote(key) + "[\"']?\\s*:");
        for (int i = 0; i < lines.size(); i++) {
            if (p.matcher(lines.get(i)).find()) {
                return i + 1;
            }
        }
        return 0;
    }

    /**
     * 1-based line of the first occurrence of {@code needle} (case-insensitive) inside the
     * block of top-level key {@code key}; falls back to the key's own line.
     */
    public int find(String key, String needle) {
        int start = key(key);
        if (start == 0 || needle == null || needle.isBlank()) {
            return start;
        }
        String n = needle.toLowerCase(Locale.ROOT);
        for (int i = start; i < lines.size(); i++) {
            String line = lines.get(i);
            if (!line.isEmpty() && !Character.isWhitespace(line.charAt(0)) && !line.startsWith("#")
                    && !line.startsWith("-")) {
                break; // next top-level key
            }
            if (line.toLowerCase(Locale.ROOT).contains(n)) {
                return i + 1;
            }
        }
        return start;
    }
}
