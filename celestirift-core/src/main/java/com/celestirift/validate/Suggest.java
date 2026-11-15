package com.celestirift.validate;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Locale;

/** "Did you mean ...?" — closest candidate by edit distance. */
public final class Suggest {

    private Suggest() {}

    /** The closest candidate within a small edit distance, or null. */
    public static @Nullable String closest(String input, Collection<String> candidates) {
        String in = input.toLowerCase(Locale.ROOT);
        int limit = Math.max(2, in.length() / 3);
        String best = null;
        int bestDist = Integer.MAX_VALUE;
        for (String c : candidates) {
            String lc = c.toLowerCase(Locale.ROOT);
            int d = (lc.startsWith(in) || in.startsWith(lc)) && Math.min(lc.length(), in.length()) >= 3
                    ? Math.abs(lc.length() - in.length()) // prefix match counts as close
                    : distance(in, lc);
            if (d < bestDist) {
                bestDist = d;
                best = c;
            }
        }
        return bestDist <= limit ? best : null;
    }

    static int distance(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] cur = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            prev[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            cur[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                cur[j] = Math.min(Math.min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] t = prev;
            prev = cur;
            cur = t;
        }
        return prev[b.length()];
    }
}
