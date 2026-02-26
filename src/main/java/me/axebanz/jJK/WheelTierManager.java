package me.axebanz.jJK;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WheelTierManager {
    private final JJKCursedToolsPlugin plugin;
    private final Map<UUID, Integer> tiers = new HashMap<>();
    private final int maxTier;

    public WheelTierManager(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
        this.maxTier = plugin.getConfig().getInt("wheel.max-tier", 5);
    }

    public int getTier(UUID uuid) {
        return tiers.getOrDefault(uuid, 1);
    }

    public void setTier(UUID uuid, int tier) {
        tiers.put(uuid, Math.max(1, Math.min(maxTier, tier)));
    }

    public void spin(Player player) {
        int current = getTier(player.getUniqueId());
        if (current >= maxTier) {
            player.sendMessage(plugin.cfg().prefix() + "§6Already at max tier!");
            return;
        }
        if (Math.random() < 0.5) {
            setTier(player.getUniqueId(), current + 1);
            player.sendMessage(plugin.cfg().prefix() + "§aWheel spin successful! Tier: " + getTier(player.getUniqueId()));
        } else {
            player.sendMessage(plugin.cfg().prefix() + "§cWheel spin failed.");
        }
    }
}
