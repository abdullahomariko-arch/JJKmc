package me.axebanz.jJK;

import org.bukkit.entity.ItemDisplay;
import org.bukkit.scheduler.BukkitTask;

/**
 * Per-player session data for the Granite Blast ability.
 * Tracks charge state, timing, active tasks, and display entities.
 */
public final class GraniteBlastSession {

    /** States a Granite Blast session can be in. */
    public enum ChargeState {
        IDLE, CHARGING, FIRING, CANCELLED
    }

    /** Charge tier thresholds. */
    public enum ChargeTier {
        LOW, MEDIUM, HIGH
    }

    /** Maximum charge duration in milliseconds (3 seconds = full charge). */
    public static final long MAX_CHARGE_MS = 3000L;

    private ChargeState state = ChargeState.IDLE;
    private long chargeStartTime = 0L;
    private int chargePercent = 0;

    // Display entities
    ItemDisplay chargeVisualEntity = null;
    ItemDisplay beamEntity = null;

    // Active tasks
    BukkitTask chargingTask = null;
    BukkitTask beamTask = null;
    BukkitTask spiralTask = null;
    BukkitTask projectileTask = null;

    public GraniteBlastSession() {}

    /** Transitions from IDLE → CHARGING and records the start time. */
    public void startCharging() {
        state = ChargeState.CHARGING;
        chargeStartTime = System.currentTimeMillis();
        chargePercent = 0;
    }

    /** Recalculates and updates chargePercent based on elapsed time. */
    public void updateChargePercent() {
        if (state != ChargeState.CHARGING) return;
        long elapsed = System.currentTimeMillis() - chargeStartTime;
        chargePercent = (int) Math.min(100, (elapsed * 100L) / MAX_CHARGE_MS);
    }

    /**
     * Finalises charging and returns the determined tier.
     * Thresholds: &lt;33% → LOW, 33–74% → MEDIUM, 75%+ → HIGH
     */
    public ChargeTier release() {
        updateChargePercent();
        state = ChargeState.FIRING;
        if (chargePercent < 33) return ChargeTier.LOW;
        if (chargePercent < 75) return ChargeTier.MEDIUM;
        return ChargeTier.HIGH;
    }

    /** Marks this session as CANCELLED. */
    public void cancel() {
        state = ChargeState.CANCELLED;
    }

    /** Marks this session as back to IDLE (ability finished). */
    public void finishAbility() {
        state = ChargeState.IDLE;
    }

    /** Cancels all running tasks and removes all display entities without changing state. */
    public void cleanup() {
        if (chargingTask != null) { chargingTask.cancel(); chargingTask = null; }
        if (beamTask != null)     { beamTask.cancel();     beamTask = null; }
        if (spiralTask != null)   { spiralTask.cancel();   spiralTask = null; }
        if (projectileTask != null) { projectileTask.cancel(); projectileTask = null; }

        if (chargeVisualEntity != null && chargeVisualEntity.isValid()) {
            chargeVisualEntity.remove();
        }
        chargeVisualEntity = null;

        if (beamEntity != null && beamEntity.isValid()) {
            beamEntity.remove();
        }
        beamEntity = null;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public ChargeState getState() { return state; }
    public int getChargePercent() { return chargePercent; }
    public boolean isCharging() { return state == ChargeState.CHARGING; }
}
