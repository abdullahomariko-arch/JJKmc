package me.axebanz.jJK;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WheelUI {
    private final JJKCursedToolsPlugin plugin;
    private final WheelTierManager tierManager;

    public WheelUI(JJKCursedToolsPlugin plugin, WheelTierManager tierManager) {
        this.plugin = plugin;
        this.tierManager = tierManager;
    }

    public void showStatus(Player player) {
        int tier = tierManager.getTier(player.getUniqueId());
        player.sendMessage(plugin.cfg().prefix() + "§6=== Wheel Status ===");
        player.sendMessage("§7Tier: §e" + tier);
    }
}
