package me.axebanz.jJK;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class WheelTierManager {

    // Keep plugin optional so BOTH constructors work (prevents "expected 1 argument" errors)
    private final JJKCursedToolsPlugin plugin;

    private final Map<UUID, WheelAdaptationState> states = new ConcurrentHashMap<>();

    /** Backwards-compatible no-arg constructor */
    public WheelTierManager() {
        this.plugin = null;
    }

    /** Preferred constructor */
    public WheelTierManager(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    public WheelAdaptationState getOrCreate(UUID uuid) {
        return states.computeIfAbsent(uuid, WheelAdaptationState::new);
    }

    /**
     * Record an adaptation "hit" for any category.
     * Use this for normal damage AND for manual true-damage (setHealth).
     */
    public void recordCategoryHit(UUID uuid, AdaptationCategory category, double damageDealt, Player victim) {
        WheelAdaptationState state = getOrCreate(uuid);

        // Must survive the hit (optional rule)
        if (victim.getHealth() - damageDealt <= 0) return;

        state.addStack(category);

        int stacks = state.getStacks(category);

        // Heal starts after 5 stacks (your described behavior)
        if (stacks >= 5) {
            double healAmount = state.getHealAmount(category);
            double newHealth = Math.min(victim.getHealth() + healAmount, victim.getMaxHealth());
            victim.setHealth(newHealth);
        }
    }

    public void recordHit(UUID uuid, AdaptationCategory category, double damageDealt, Player victim) {
        recordCategoryHit(uuid, category, damageDealt, victim);
    }

    public double getDamageReduction(UUID uuid) {
        return getOrCreate(uuid).getDamageReduction(getOrCreate(uuid).getCurrentMode());
    }

    public int getStacks(UUID uuid, AdaptationCategory category) {
        return getOrCreate(uuid).getStacks(category);
    }

    public AdaptationCategory getCurrentMode(UUID uuid) {
        return getOrCreate(uuid).getCurrentMode();
    }

    public void setMode(UUID uuid, AdaptationCategory mode) {
        getOrCreate(uuid).setCurrentMode(mode);
    }

    public void shuffleMode(UUID uuid) {
        getOrCreate(uuid).shuffleMode();
    }

    public void clearState(UUID uuid) {
        states.remove(uuid);
    }
}