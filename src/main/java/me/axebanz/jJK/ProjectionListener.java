package me.axebanz.jJK;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

public final class ProjectionListener implements Listener {

    private final JJKCursedToolsPlugin plugin;
    private final ProjectionManager projectionManager;

    public ProjectionListener(JJKCursedToolsPlugin plugin, ProjectionManager projectionManager) {
        this.plugin = plugin;
        this.projectionManager = projectionManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamageTaken(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player victim)) return;
        projectionManager.onTakeDamage(victim);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamageByEntity(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player attacker)) return;

        // === If target is frozen: shatter on hit ===
        if (e.getEntity() instanceof LivingEntity victim) {
            if (projectionManager.getFreezeHandler().isFrozen(victim.getUniqueId())) {
                e.setCancelled(true);
                projectionManager.getFreezeHandler().normalShatter(attacker, victim.getUniqueId());
                return;
            }
        }

        // Auto lock-on for projection users
        String assignedId = plugin.techniqueManager().getAssignedId(attacker.getUniqueId());
        if ("projection".equalsIgnoreCase(assignedId)) {
            if (e.getEntity() instanceof LivingEntity target && !target.equals(attacker)) {
                projectionManager.autoLockOnHit(attacker, target);
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();

        // Frozen players can't move (only look around)
        if (projectionManager.getFreezeHandler().isFrozen(p.getUniqueId())) {
            if (e.getFrom().getBlockX() != e.getTo().getBlockX()
                    || e.getFrom().getBlockZ() != e.getTo().getBlockZ()
                    || e.getFrom().getBlockY() != e.getTo().getBlockY()) {
                e.setTo(e.getFrom());
            }
            return;
        }

        ProjectionPlayerData data = projectionManager.getData(p.getUniqueId());
        if (data == null) return;

        if (data.state == ProjectionState.FROZEN_PENALTY) {
            if (e.getFrom().getBlockX() != e.getTo().getBlockX()
                    || e.getFrom().getBlockZ() != e.getTo().getBlockZ()
                    || e.getFrom().getBlockY() != e.getTo().getBlockY()) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        projectionManager.cleanup(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        projectionManager.cleanup(e.getPlayer().getUniqueId());
    }
}