package me.axebanz.jJK;

import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public final class BloodManipulationListener implements Listener {

    private final JJKCursedToolsPlugin plugin;
    private final BloodManipulationManager mgr;

    public BloodManipulationListener(JJKCursedToolsPlugin plugin, BloodManipulationManager mgr) {
        this.plugin = plugin;
        this.mgr = mgr;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player p = event.getPlayer();
        if (!CursedWombItem.isCursedWomb(event.getItem())) return;
        String id = plugin.techniqueManager().getAssignedId(p.getUniqueId());
        if (!"blood_manipulation".equalsIgnoreCase(id)) {
            p.sendMessage(plugin.cfg().prefix() + "§cYou need §4Blood Manipulation §cto consume this.");
            return;
        }
        event.setCancelled(true);
        if (event.getItem().getAmount() > 1) {
            event.getItem().setAmount(event.getItem().getAmount() - 1);
        } else {
            p.getInventory().setItemInMainHand(null);
        }
        mgr.consumeWomb(p);
    }

    @EventHandler
    public void onProjectileHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Arrow arrow)) return;
        if (!mgr.isHarpoonArrow(arrow.getUniqueId())) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;
        if (!(arrow.getShooter() instanceof Player shooter)) return;

        event.setCancelled(true);
        mgr.applyHarpoonHit(target, arrow.getUniqueId(), shooter);
        arrow.remove();
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;
        // Schedule next tick so damage is applied before RCT check
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (p.isOnline()) mgr.checkBloodRCT(p);
        }, 1L);
    }
}
