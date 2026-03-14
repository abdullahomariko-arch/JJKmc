package me.axebanz.jJK;

import java.util.UUID;

public final class WheelStateSerializer {

    private final JJKCursedToolsPlugin plugin;

    public WheelStateSerializer(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Current wheel state (WheelAdaptationState / WheelTierManager) is in-memory only.
     * WheelTierManager is not persisted to PlayerProfile yet, so we intentionally do nothing.
     */
    public void saveState(UUID uuid, WheelAdaptationState state, PlayerProfile profile) {
        // no-op
    }

    /**
     * Current wheel resets each session; no persisted state is loaded.
     */
    public void loadState(UUID uuid, WheelAdaptationState state, PlayerProfile profile) {
        // no-op
    }
}