package me.axebanz.jJK;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class WheelAdaptationState {

    private final UUID playerUuid;
    private AdaptationCategory currentMode = AdaptationCategory.MELEE;
    private final Map<AdaptationCategory, Integer> stacks = new HashMap<>();

    public WheelAdaptationState(UUID playerUuid) {
        this.playerUuid = playerUuid;
        for (AdaptationCategory cat : AdaptationCategory.values()) {
            stacks.put(cat, 0);
        }
    }

    public AdaptationCategory getCurrentMode() { return currentMode; }

    public void setCurrentMode(AdaptationCategory mode) {
        this.currentMode = mode;
    }

    public void shuffleMode() {
        this.currentMode = currentMode.next();
    }

    public int getStacks(AdaptationCategory mode) {
        return stacks.getOrDefault(mode, 0);
    }

    public void addStack(AdaptationCategory mode) {
        int current = getStacks(mode);
        if (current < 100) stacks.put(mode, current + 1);
    }

    public double getDamageReduction(AdaptationCategory category) {
        int stackCount = getStacks(category);
        return stackCount * 0.008; // 0.8% per stack, max 80%
    }

    public double getHealAmount(AdaptationCategory category) {
        int stackCount = getStacks(category);
        return stackCount * 0.2; // same as your existing curve
    }
}