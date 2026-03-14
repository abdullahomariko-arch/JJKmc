package me.axebanz.jJK;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.UUID;

public final class TenShadowsListener implements Listener {

    private final JJKCursedToolsPlugin plugin;
    private final TenShadowsManager manager;

    public TenShadowsListener(JJKCursedToolsPlugin plugin, TenShadowsManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDeath(EntityDeathEvent e) {
        Entity entity = e.getEntity();

        if (!manager.isShikigamiEntity(entity)) return;

        UUID ownerUuid = manager.getShikigamiOwner(entity);
        if (ownerUuid == null) return;

        ShikigamiType type = manager.getShikigamiType(entity);
        if (type == null) return;

        if (manager.isRitualMob(entity)) {
            manager.onRitualMobDeath(ownerUuid, type);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamageShikigami(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player attacker)) return;

        Entity victim = e.getEntity();
        if (!manager.isShikigamiEntity(victim)) return;

        UUID ownerUuid = manager.getShikigamiOwner(victim);
        if (ownerUuid == null) return;

        ShikigamiType type = manager.getShikigamiType(victim);

        // ArmorStand-based shikigami (Mahoraga) — handle damage manually
        if (type != null && type.usesArmorStandModel()) {
            e.setCancelled(true); // ArmorStands don't take damage normally

            // Only allow damage during ritual (from owner) or from enemies (when friendly)
            boolean isRitual = manager.isRitualMob(victim);

            if (isRitual && attacker.getUniqueId().equals(ownerUuid)) {
                // Owner attacking ritual Mahoraga — deal damage
                double damage = e.getDamage();
                manager.damageArmorStandShikigami(ownerUuid, damage);

                // Track phenomenon for adaptation
                // (simplified — attacker weapon type)
                attacker.getWorld().playSound(victim.getLocation(), org.bukkit.Sound.ENTITY_IRON_GOLEM_HURT, 0.8f, 1.0f);
            } else if (!isRitual && !attacker.getUniqueId().equals(ownerUuid)) {
                // Enemy attacking friendly Mahoraga — deal damage
                double damage = e.getDamage();
                manager.damageArmorStandShikigami(ownerUuid, damage);
                attacker.getWorld().playSound(victim.getLocation(), org.bukkit.Sound.ENTITY_IRON_GOLEM_HURT, 0.8f, 1.0f);
            }
            return;
        }

        // Normal mob shikigami
        if (manager.isRitualMob(victim)) return; // Allow damage to ritual mobs

        // Block damage to friendly shikigami from their owner
        if (attacker.getUniqueId().equals(ownerUuid)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        manager.cleanup(uuid);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        manager.cleanup(uuid);
    }
}