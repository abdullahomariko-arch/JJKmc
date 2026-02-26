package me.axebanz.jJK;

import org.bukkit.entity.Player;

public class RikaManager {
    private final JJKCursedToolsPlugin plugin;

    public RikaManager(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    public void summonRika(Player player) {
        player.sendMessage(plugin.cfg().prefix() + "§5Rika summoned!");
    }

    public void dismissRika(Player player) {
        player.sendMessage(plugin.cfg().prefix() + "§5Rika dismissed.");
    }
}
