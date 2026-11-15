package com.celestirift.pack;

import dev.orionrp.api.OrionRP;
import org.bukkit.plugin.Plugin;

import java.nio.file.Path;

/**
 * Talks to OrionRP. Only touched after checking that OrionRP is enabled, so this class
 * (and the OrionRP API it references) is never loaded on servers without it.
 */
final class OrionRPHook {

    static final String SOURCE_ID = "celestirift";
    /** Below the server's own OrionRP assets folder (1000), so admins can override our files. */
    static final int PRIORITY = 100;

    private OrionRPHook() {
    }

    /** Register (or re-register) the bundled pack. Returns false if OrionRP isn't available. */
    static boolean register(Plugin plugin, Path zip) {
        if (!OrionRP.available()) {
            return false;
        }
        OrionRP.get().registerZip(SOURCE_ID, plugin, zip, PRIORITY);
        return true;
    }

    static void unregister() {
        if (OrionRP.available()) {
            OrionRP.get().unregister(SOURCE_ID);
        }
    }

    /** Whether OrionRP sends the merged pack to players. */
    static boolean sendsPack() {
        return OrionRP.available() && OrionRP.get().sendsPack();
    }
}
