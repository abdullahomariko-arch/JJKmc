package me.axebanz.jJK;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

public final class WheelCombatHandler {

    private final JJKCursedToolsPlugin plugin;
    private final WheelTierManager tierManager;
    private final WheelUI ui;

    public WheelCombatHandler(JJKCursedToolsPlugin plugin, PhenomenonDetector detector, WheelTierManager tierManager, WheelUI ui) {
        this.plugin = plugin;
        this.tierManager = tierManager;
        this.ui = ui;
    }

    /** Damage caused by entities (players, mobs, arrows, etc.) */
    public void handleWheelDefense(EntityDamageByEntityEvent e, Player victim) {
        if (e.isCancelled()) return;
        if (!isWearingWheel(victim)) return;

        AdaptationCategory incoming = detectCategory(e.getCause());
        applyWheel(e, victim, incoming);
    }

    /** Damage caused by environment (fire tick, lava, hot floor, etc.) */
    public void handleWheelDefense(EntityDamageEvent e, Player victim) {
        if (e.isCancelled()) return;
        if (!isWearingWheel(victim)) return;

        AdaptationCategory incoming = detectCategory(e.getCause());
        applyWheel(e, victim, incoming);
    }

    private void applyWheel(EntityDamageEvent e, Player victim, AdaptationCategory incomingCategory) {
        WheelAdaptationState state = tierManager.getOrCreate(victim.getUniqueId());

        // AUTO-ADAPT DEFENSE:
        // Reduction should be based on the INCOMING category stacks, not current mode.
        double reduction = state.getDamageReduction(incomingCategory);

        double newDamage = e.getDamage() * (1.0 - reduction);
        e.setDamage(Math.max(0.5, newDamage));

        // record hit/stacks for the incoming category
        tierManager.recordHit(victim.getUniqueId(), incomingCategory, newDamage, victim);

        // UI stays as you designed: show CURRENT MODE stacks (not necessarily incoming)
        int stacks = tierManager.getStacks(victim.getUniqueId(), state.getCurrentMode());
        ui.showStackGain(victim, state.getCurrentMode(), stacks, 100);
    }

    private AdaptationCategory detectCategory(EntityDamageEvent.DamageCause cause) {
        return switch (cause) {
            case PROJECTILE -> AdaptationCategory.PROJECTILE;
            case ENTITY_EXPLOSION, BLOCK_EXPLOSION -> AdaptationCategory.EXPLOSION;
            case FIRE, FIRE_TICK, LAVA, HOT_FLOOR -> AdaptationCategory.FIRE;
            case LIGHTNING -> AdaptationCategory.LIGHTNING;
            default -> AdaptationCategory.MELEE;
        };
    }

    private boolean isWearingWheel(Player p) {
        ItemStack helmet = p.getInventory().getHelmet();
        return plugin.tools().identify(helmet) == ToolId.DIVINE_WHEEL;
    }
}