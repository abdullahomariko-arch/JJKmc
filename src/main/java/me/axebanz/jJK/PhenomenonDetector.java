package me.axebanz.jJK;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

public final class PhenomenonDetector {

    private final JJKCursedToolsPlugin plugin;

    public PhenomenonDetector(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    public PhenomenonType detect(EntityDamageByEntityEvent e) {
        Entity damager = e.getDamager();

        // Check tool first
        if (damager instanceof org.bukkit.entity.Player p) {
            ItemStack mainHand = p.getInventory().getItemInMainHand();
            ToolId toolId = plugin.tools().identify(mainHand);

            if (toolId == ToolId.DRAGON_BONE) return PhenomenonType.SLASHING;
            if (toolId == ToolId.SPLIT_SOUL_KATANA) return PhenomenonType.TRUE_DAMAGE;
            if (toolId == ToolId.KAMUTOKE) return PhenomenonType.ELECTRICAL;
            if (toolId == ToolId.INVERTED_SPEAR) return PhenomenonType.NEGATION;
        }

        // Check damage cause
        EntityDamageEvent.DamageCause cause = e.getCause();

        if (cause == EntityDamageEvent.DamageCause.FIRE
                || cause == EntityDamageEvent.DamageCause.FIRE_TICK
                || cause == EntityDamageEvent.DamageCause.LAVA
                || cause == EntityDamageEvent.DamageCause.HOT_FLOOR) {
            return PhenomenonType.THERMAL;
        }

        if (cause == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION
                || cause == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {
            return PhenomenonType.BLUNT;
        }

        if (cause == EntityDamageEvent.DamageCause.LIGHTNING) {
            return PhenomenonType.ELECTRICAL;
        }

        if (cause == EntityDamageEvent.DamageCause.FREEZE) {
            return PhenomenonType.FREEZING;
        }

        if (damager instanceof Projectile) {
            return PhenomenonType.PIERCING;
        }

        // Default to SLASHING for melee hits
        return PhenomenonType.SLASHING;
    }
}