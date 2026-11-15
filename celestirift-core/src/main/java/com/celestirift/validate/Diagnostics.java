package com.celestirift.validate;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Problems found while loading content (skills, gems, fusions). Each entry points to
 * a file and, when it can be found, a line number, so authors can jump straight to it.
 * Collected on every reload and shown by {@code /cr validate}.
 */
public final class Diagnostics {

    public enum Severity { ERROR, WARNING }

    /**
     * @param severity ERROR = the item was skipped/broken, WARNING = loaded with a fallback
     * @param file     path relative to the plugin folder
     * @param line     1-based line, or 0 when unknown
     * @param context  skill/gem/fusion id
     */
    public record Entry(Severity severity, String file, int line, String context, String message) {
        public String location() {
            return line > 0 ? file + ":" + line : file;
        }

        @Override
        public String toString() {
            return "[" + location() + "] " + context + ": " + message;
        }
    }

    private final List<Entry> entries = new ArrayList<>();

    public void clear() {
        entries.clear();
    }

    public void error(String file, int line, String context, String message) {
        entries.add(new Entry(Severity.ERROR, file, line, context, message));
    }

    public void warn(String file, int line, String context, String message) {
        entries.add(new Entry(Severity.WARNING, file, line, context, message));
    }

    /** Warn, appending a "did you mean" hint when one exists. */
    public void warn(String file, int line, String context, String message, @Nullable String suggestion) {
        warn(file, line, context, suggestion == null ? message : message + " - did you mean '" + suggestion + "'?");
    }

    /** Context used for problems that stop a whole file from loading (syntax errors, unreadable). */
    public static final String FILE = "(file)";

    /** Files that could not be loaded at all - a reload with these would lose content. */
    public List<Entry> brokenFiles() {
        return entries.stream().filter(e -> e.severity() == Severity.ERROR && FILE.equals(e.context())).toList();
    }

    public List<Entry> entries() {
        return List.copyOf(entries);
    }

    public long count(Severity severity) {
        return entries.stream().filter(e -> e.severity() == severity).count();
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /** Log every entry to the console. */
    public void log(Logger logger) {
        for (Entry e : entries) {
            if (e.severity() == Severity.ERROR) {
                logger.severe(e.toString());
            } else {
                logger.warning(e.toString());
            }
        }
    }
}
