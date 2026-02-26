package me.axebanz.jJK;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Public API access point for JJKmc.
 */
public class api {
    private static JJKCursedToolsPlugin instance;

    static void init(JJKCursedToolsPlugin plugin) {
        instance = plugin;
    }

    public static JJKCursedToolsPlugin get() {
        return instance;
    }

    public static TechniqueManager techniqueManager() {
        return instance.techniqueManager();
    }

    public static CursedEnergyManager ceManager() {
        return instance.ceManager();
    }
}
