package me.axebanz.jJK;

import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ProjectionPlayerData {

    public final UUID uuid;

    public ProjectionState state = ProjectionState.IDLE;

    /** Current step index within the ACTIVE phase */
    public int stepIndex = 0;

    /** Ticks remaining in current state phase */
    public int phaseTicks = 0;

    /** Pre-computed path of positions */
    public final List<Location> path = new ArrayList<>();

    /** Momentum stacks (0-3) */
    public int stacks = 0;

    /** Direction committed to during PROGRAMMING phase */
    public Vector commitDir = null;

    /** Locked target UUID for auto lock-on */
    public UUID lockedTarget = null;

    /** Task ID for the lock-on particle beam task (-1 if none) */
    public int lockParticleTask = -1;

    /** Whether player has a frozen target (for Breaker) */
    public UUID frozenTarget = null;

    public ProjectionPlayerData(UUID uuid) {
        this.uuid = uuid;
    }

    public void reset() {
        state = ProjectionState.IDLE;
        stepIndex = 0;
        phaseTicks = 0;
        path.clear();
        commitDir = null;
    }
}
