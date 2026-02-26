package me.axebanz.jJK;

import org.bukkit.entity.Player;

public class IdleDeathGambleManager {
    private final JJKCursedToolsPlugin plugin;
    private final CooldownManager cooldowns;
    private final DomainManager domainManager;

    public IdleDeathGambleManager(JJKCursedToolsPlugin plugin, CooldownManager cooldowns, DomainManager domainManager) {
        this.plugin = plugin;
        this.cooldowns = cooldowns;
        this.domainManager = domainManager;
    }

    public void triggerGamble(Player player) {
        long cd = plugin.getConfig().getLong("idle-death-gamble.gamble-cooldown", 30000L);
        if (cooldowns.isOnCooldown(player.getUniqueId(), "idg_gamble")) {
            long rem = cooldowns.getRemaining(player.getUniqueId(), "idg_gamble");
            player.sendMessage(plugin.cfg().prefix() + "§cGamble on cooldown: " + (rem / 1000) + "s");
            return;
        }
        cooldowns.setCooldown(player.getUniqueId(), "idg_gamble", cd);
        double roll = Math.random();
        if (roll < 0.5) {
            player.sendMessage(plugin.cfg().prefix() + "§aThe gamble pays off! §7Roll: " + String.format("%.2f", roll));
        } else {
            player.sendMessage(plugin.cfg().prefix() + "§cThe gamble fails... §7Roll: " + String.format("%.2f", roll));
            player.setHealth(Math.max(1.0, player.getHealth() / 2.0));
        }
    }

    public void openDomain(Player player) {
        if (domainManager.hasDomain(player.getUniqueId())) {
            player.sendMessage(plugin.cfg().prefix() + "§cDomain already active.");
            return;
        }
        IdleDeathGambleDomain domain = new IdleDeathGambleDomain(plugin, player.getUniqueId(), player.getLocation());
        domainManager.openDomain(player, domain);
    }
}
