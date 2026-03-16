package me.axebanz.jJK;

import org.bukkit.Material;
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
import org.bukkit.inventory.ItemStack;

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

        // ArmorStand-based shikigami (legacy) — handle damage manually
        if (type != null && type.usesArmorStandModel()) {
            e.setCancelled(true);

            boolean isRitual = manager.isRitualMob(victim);
            if (isRitual && attacker.getUniqueId().equals(ownerUuid)) {
                double damage = e.getDamage();
                manager.damageArmorStandShikigami(ownerUuid, damage);
                attacker.getWorld().playSound(victim.getLocation(), org.bukkit.Sound.ENTITY_IRON_GOLEM_HURT, 0.8f, 1.0f);
            } else if (!isRitual && !attacker.getUniqueId().equals(ownerUuid)) {
                double damage = e.getDamage();
                manager.damageArmorStandShikigami(ownerUuid, damage);
                attacker.getWorld().playSound(victim.getLocation(), org.bukkit.Sound.ENTITY_IRON_GOLEM_HURT, 0.8f, 1.0f);
            }
            return;
        }

        // Mahoraga (Iron Golem) — track phenomenon for adaptation when it's attacked
        if (type == ShikigamiType.MAHORAGA) {
            String phenomenon = detectPhenomenon(attacker);
            manager.notifyMahoragaHit(ownerUuid, phenomenon, e.getDamage());
        }

        // Normal mob shikigami
        if (manager.isRitualMob(victim)) return; // Allow damage to ritual mobs

        // Block damage to friendly shikigami from their owner
        if (attacker.getUniqueId().equals(ownerUuid)) {
            e.setCancelled(true);
        }
    }

    /**
     * Detect the phenomenon type based on attacker's weapon/technique.
     */
    private String detectPhenomenon(Player attacker) {
        ItemStack weapon = attacker.getInventory().getItemInMainHand();
        Material mat = weapon.getType();

        // Check if attacker has a cursed technique
        String techId = plugin.techniqueManager().getAssignedId(attacker.getUniqueId());
        if (techId != null && !techId.isEmpty()) {
            return "technique:" + techId;
        }

        // Classify by weapon material
        if (mat.name().contains("SWORD") || mat.name().contains("AXE")) {
            return "melee:" + mat.name().toLowerCase();
        }
        if (mat == Material.BOW || mat == Material.CROSSBOW) {
            return "bow";
        }
        if (mat == Material.MACE) {
            return "mace";
        }
        if (mat == Material.AIR || mat.name().contains("FIST")) {
            return "unarmed";
        }
        return "melee:generic";
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