package me.axebanz.jJK;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ProjectionFreezeHandler {
    private final Map<UUID, Integer> frozenTicks = new HashMap<>();
    private final JJKCursedToolsPlugin plugin;

    public ProjectionFreezeHandler(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    public void freeze(Player player, int ticks) {
        frozenTicks.put(player.getUniqueId(), ticks);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, ticks, 10, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, ticks, 128, false, false));
    }

    public boolean isFrozen(UUID uuid) {
        return frozenTicks.containsKey(uuid);
    }

    public void tickFreeze(Player player) {
        UUID uuid = player.getUniqueId();
        Integer ticks = frozenTicks.get(uuid);
        if (ticks == null) return;
        ticks--;
        if (ticks <= 0) {
            frozenTicks.remove(uuid);
            player.removePotionEffect(PotionEffectType.SLOWNESS);
            player.removePotionEffect(PotionEffectType.JUMP_BOOST);
        } else {
            frozenTicks.put(uuid, ticks);
        }
    }

    public void unfreeze(Player player) {
        frozenTicks.remove(player.getUniqueId());
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.removePotionEffect(PotionEffectType.JUMP_BOOST);
    }
}
