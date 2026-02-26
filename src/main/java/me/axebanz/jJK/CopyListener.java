package me.axebanz.jJK;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.Random;

/**
 * Handles Cursed Body drop mechanics.
 * Bug Fix #4: DROP_CHANCE changed from 0.50 to 0.05 (5%).
 */
public class CopyListener implements Listener {
    // Bug Fix #4: 5% drop chance
    private static final double DROP_CHANCE = 0.05;

    private final JJKCursedToolsPlugin plugin;
    private final CopyManager copyManager;
    private final Random random = new Random();

    public CopyListener(JJKCursedToolsPlugin plugin, CopyManager copyManager) {
        this.plugin = plugin;
        this.copyManager = copyManager;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        Entity target = event.getEntity();

        if (!(damager instanceof Player attacker)) return;
        if (!(target instanceof Player victim)) return;

        // Chance to drop a cursed body on hit
        if (random.nextDouble() < DROP_CHANCE) {
            copyManager.handleCursedBodyDrop(victim, attacker);
        }
    }
}
