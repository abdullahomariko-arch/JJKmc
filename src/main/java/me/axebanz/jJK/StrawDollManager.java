package me.axebanz.jJK;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class StrawDollManager {
    private final JJKCursedToolsPlugin plugin;
    private final CooldownManager cooldowns;
    private final Set<UUID> bindingVows = new HashSet<>();

    public StrawDollManager(JJKCursedToolsPlugin plugin, CooldownManager cooldowns) {
        this.plugin = plugin;
        this.cooldowns = cooldowns;
    }

    public void activateResonance(Player player) {
        long cd = plugin.getConfig().getLong("straw-doll.resonance-cooldown", 10000L);
        if (cooldowns.isOnCooldown(player.getUniqueId(), "resonance")) {
            long rem = cooldowns.getRemaining(player.getUniqueId(), "resonance");
            player.sendMessage(plugin.cfg().prefix() + "§cResonance on cooldown: " + (rem / 1000) + "s");
            return;
        }
        cooldowns.setCooldown(player.getUniqueId(), "resonance", cd);
        player.sendMessage(plugin.cfg().prefix() + "§6Resonance activated!");
    }

    public void activateHairpin(Player player) {
        long cd = plugin.getConfig().getLong("straw-doll.hairpin-cooldown", 15000L);
        if (cooldowns.isOnCooldown(player.getUniqueId(), "hairpin")) {
            long rem = cooldowns.getRemaining(player.getUniqueId(), "hairpin");
            player.sendMessage(plugin.cfg().prefix() + "§cHairpin on cooldown: " + (rem / 1000) + "s");
            return;
        }
        cooldowns.setCooldown(player.getUniqueId(), "hairpin", cd);
        player.sendMessage(plugin.cfg().prefix() + "§6Hairpin detonated!");
    }

    public boolean hasBindingVow(UUID uuid) {
        return bindingVows.contains(uuid);
    }

    public void addBindingVow(UUID uuid) {
        bindingVows.add(uuid);
    }

    public void removeBindingVow(UUID uuid) {
        bindingVows.remove(uuid);
    }
}
