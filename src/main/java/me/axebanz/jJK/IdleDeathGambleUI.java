package me.axebanz.jJK;

import org.bukkit.entity.Player;

public class IdleDeathGambleUI {
    private final JJKCursedToolsPlugin plugin;

    public IdleDeathGambleUI(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    public void showStatus(Player player) {
        player.sendMessage(plugin.cfg().prefix() + "§e=== Idle Death Gamble ===");
        player.sendMessage("§7Use /idledeathgamble gamble or /idledeathgamble domain");
    }
}
