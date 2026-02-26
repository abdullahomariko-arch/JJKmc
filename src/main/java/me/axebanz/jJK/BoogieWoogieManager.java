package me.axebanz.jJK;

import org.bukkit.entity.Player;

public class BoogieWoogieManager {
    private final JJKCursedToolsPlugin plugin;
    private final CooldownManager cooldowns;

    public BoogieWoogieManager(JJKCursedToolsPlugin plugin, CooldownManager cooldowns) {
        this.plugin = plugin;
        this.cooldowns = cooldowns;
    }

    public void activateSwap(Player player) {
        if (cooldowns.isOnCooldown(player.getUniqueId(), "bw_swap")) {
            long rem = cooldowns.getRemaining(player.getUniqueId(), "bw_swap");
            player.sendMessage(plugin.cfg().prefix() + "§cSwap on cooldown: " + (rem / 1000) + "s");
            return;
        }
        cooldowns.setCooldown(player.getUniqueId(), "bw_swap", 5000L);
        player.sendMessage(plugin.cfg().prefix() + "§eBoogie Woogie - Swap!");
    }
}
