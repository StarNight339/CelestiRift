package com.celestirift.gem;

import java.util.Locale;

/** A gem/socket is either ACTIVE (grants a skill) or PASSIVE (augments the weapon's skills). */
public enum SocketType {
    ACTIVE,
    PASSIVE;

    public static SocketType fromString(String s, SocketType def) {
        if (s == null) {
            return def;
        }
        try {
            return SocketType.valueOf(s.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return def;
        }
    }
}
