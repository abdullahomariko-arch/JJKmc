package me.axebanz.jJK;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class EnergyDischargeListener implements Listener {

    private final JJKCursedToolsPlugin plugin;

    public EnergyDischargeListener(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    /** Passive — Enhanced Strikes: 1.5× melee damage + blue particles. */
    @EventHandler
    public void onMeleeHit(EntityDamageByEntityEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof LivingEntity victim)) return;

        EnergyDischargeManager mgr = plugin.energyDischarge();
        if (mgr == null) return;
        if (!mgr.hasTechnique(attacker)) return;

        // Don't double-apply if attacker is the same as victim
        if (attacker.getUniqueId().equals(victim.getUniqueId())) return;

        mgr.applyStrikePassive(attacker, victim, event.getDamage(), event);
    }

    /** Passive — Enhanced Strikes: 20% less damage taken. */
    @EventHandler
    public void onPlayerDamaged(EntityDamageEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getEntity() instanceof Player victim)) return;

        EnergyDischargeManager mgr = plugin.energyDischarge();
        if (mgr == null) return;
        if (!mgr.hasTechnique(victim)) return;

        mgr.applyDamageReduction(event);
    }

    /** Phase 3 lock — prevent movement while locked in place. */
    @EventHandler
    public void onMoveLocked(PlayerMoveEvent event) {
        EnergyDischargeManager mgr = plugin.energyDischarge();
        if (mgr == null) return;
        if (!mgr.isLocked(event.getPlayer().getUniqueId())) return;

        // Only cancel horizontal movement; allow head rotation for Phase 1 & 2
        if (event.getFrom().getBlockX() != event.getTo().getBlockX()
                || event.getFrom().getBlockY() != event.getTo().getBlockY()
                || event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
            event.setTo(event.getFrom().setDirection(event.getTo().getDirection()));
        }
    }

    /** Cleanup on logout. */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        EnergyDischargeManager mgr = plugin.energyDischarge();
        if (mgr != null) mgr.cleanup(event.getPlayer().getUniqueId());
    }
}
