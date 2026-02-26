package me.axebanz.jJK;

import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ProjectionPlayerData {
    private final UUID uuid;
    private ProjectionState state = ProjectionState.IDLE;
    private Location programmingStart;
    private Vector programmedDirection;
    private double programmedDistance;
    private int frozenTicks;
    private int maxFrozenTicks;
    private Location breakerOrigin;
    private int stepIndex;
    private List<Location> path = new ArrayList<>();

    public ProjectionPlayerData(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() { return uuid; }
    public ProjectionState getState() { return state; }
    public void setState(ProjectionState state) { this.state = state; }
    public Location getProgrammingStart() { return programmingStart; }
    public void setProgrammingStart(Location loc) { this.programmingStart = loc; }
    public Vector getProgrammedDirection() { return programmedDirection; }
    public void setProgrammedDirection(Vector dir) { this.programmedDirection = dir; }
    public double getProgrammedDistance() { return programmedDistance; }
    public void setProgrammedDistance(double dist) { this.programmedDistance = dist; }
    public int getFrozenTicks() { return frozenTicks; }
    public void setFrozenTicks(int ticks) { this.frozenTicks = ticks; }
    public int getMaxFrozenTicks() { return maxFrozenTicks; }
    public void setMaxFrozenTicks(int ticks) { this.maxFrozenTicks = ticks; }
    public Location getBreakerOrigin() { return breakerOrigin; }
    public void setBreakerOrigin(Location loc) { this.breakerOrigin = loc; }
    public int getStepIndex() { return stepIndex; }
    public void setStepIndex(int idx) { this.stepIndex = idx; }
    public List<Location> getPath() { return path; }
    public void setPath(List<Location> path) { this.path = path; }
    public void reset() {
        state = ProjectionState.IDLE;
        programmingStart = null;
        programmedDirection = null;
        programmedDistance = 0;
        frozenTicks = 0;
        maxFrozenTicks = 0;
        breakerOrigin = null;
        stepIndex = 0;
        path.clear();
    }
}
