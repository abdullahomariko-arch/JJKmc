package me.axebanz.jJK;

import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles Limitless Technique events:
 * - Infinity: blocks projectiles and melee attacks
 * - Blue Max: EntityDeathEvent marks kill → orb becomes lockable
 * - Blue Max: PlayerToggleSneakEvent to lock/unlock the orb
 */
public final class LimitlessListener implements Listener {

    private final JJKCursedToolsPlugin plugin;
    private final LimitlessManager manager;

    /** Active projectiles that are "floating" near an Infinity player */
    private final Map<UUID, Location> floatingProjectiles = new ConcurrentHashMap<>();

    public LimitlessListener(JJKCursedToolsPlugin plugin, LimitlessManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    /** Block melee attacks on Infinity players */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player victim)) return;
        if (!manager.isInfinityActive(victim)) return;

        String assignedId = plugin.techniqueManager().getAssignedId(victim.getUniqueId());
        if (!"limitless".equalsIgnoreCase(assignedId)) return;

        // Block all physical damage while Infinity is active
        e.setCancelled(true);
        victim.getWorld().spawnParticle(
                org.bukkit.Particle.ENCHANT,
                victim.getLocation().add(0, 1.0, 0),
                20, 0.4, 0.6, 0.4, 0.05
        );
    }

    /** Intercept projectiles heading toward an Infinity player */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent e) {
        if (!(e.getHitEntity() instanceof Player victim)) return;
        if (!manager.isInfinityActive(victim)) return;

        String assignedId = plugin.techniqueManager().getAssignedId(victim.getUniqueId());
        if (!"limitless".equalsIgnoreCase(assignedId)) return;

        // Cancel the hit and make the projectile "levitate"
        Projectile proj = e.getEntity();
        e.setCancelled(true);

        // Freeze the projectile in place near the player
        Location floatLoc = victim.getLocation().add(0, 1.0, 0).add(
                victim.getLocation().getDirection().multiply(-1.5)
        );
        proj.setVelocity(new Vector(0, 0, 0));
        proj.teleport(floatLoc);

        // Remove after 10 seconds
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (proj.isValid()) proj.remove();
        }, 200L);
    }

    /**
     * Blue Max kill detection — when a Limitless player kills something
     * while their Blue Max orb is active, mark the orb as lockable.
     */
    @EventHandler
    public void onEntityDeath(EntityDeathEvent e) {
        Player killer = e.getEntity().getKiller();
        if (killer == null) return;

        String assignedId = plugin.techniqueManager().getAssignedId(killer.getUniqueId());
        if (!"limitless".equalsIgnoreCase(assignedId)) return;

        // Check if they have an active Blue Max orb
        if (manager.hasActiveBluMaxOrb(killer)) {
            manager.markCanLockBlue(killer);
            killer.sendMessage(plugin.cfg().prefix() + "§b§lBlue orb §7is now lockable! §eShift to lock.");
        }
    }

    /**
     * Blue Max sneak toggle — locking and unlocking the Blue orb.
     * Lock: if player starts sneaking AND canLock → lock orb
     * Unlock: if player stops sneaking AND has locked orb → remove orb
     */
    @EventHandler
    public void onSneak(PlayerToggleSneakEvent e) {
        Player p = e.getPlayer();
        String assignedId = plugin.techniqueManager().getAssignedId(p.getUniqueId());
        if (!"limitless".equalsIgnoreCase(assignedId)) return;

        if (e.isSneaking()) {
            // Player just started sneaking — try to lock the Blue Max orb
            if (manager.hasActiveBluMaxOrb(p) && manager.canLockBlue(p)) {
                manager.lockBlueOrb(p);
            }
        } else {
            // Player stopped sneaking — release the locked Blue orb
            if (manager.hasLockedBlueOrb(p)) {
                manager.removeLockedBlueOrb(p);
            }
        }
    }

    /** Player disconnect — clean up Infinity state */
    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        String assignedId = plugin.techniqueManager().getAssignedId(p.getUniqueId());
        if ("limitless".equalsIgnoreCase(assignedId)) {
            manager.onPlayerQuit(p.getUniqueId());
        }
    }
}
