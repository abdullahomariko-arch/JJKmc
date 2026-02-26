package me.axebanz.jJK;

import org.bukkit.entity.Player;

public class CursedSpeechManager {
    private final JJKCursedToolsPlugin plugin;
    private final CooldownManager cooldowns;

    public CursedSpeechManager(JJKCursedToolsPlugin plugin, CooldownManager cooldowns) {
        this.plugin = plugin;
        this.cooldowns = cooldowns;
    }

    public void activateCommand(Player player, String command) {
        if (cooldowns.isOnCooldown(player.getUniqueId(), "cs_cmd")) {
            long rem = cooldowns.getRemaining(player.getUniqueId(), "cs_cmd");
            player.sendMessage(plugin.cfg().prefix() + "§cCommand on cooldown: " + (rem / 1000) + "s");
            return;
        }
        cooldowns.setCooldown(player.getUniqueId(), "cs_cmd", 10000L);
        player.sendMessage(plugin.cfg().prefix() + "§7Cursed Speech: \"" + command + "\"");
    }
}
