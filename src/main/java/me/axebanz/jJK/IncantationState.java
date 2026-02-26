package me.axebanz.jJK;

import java.util.UUID;

/**
 * Represents the state of an ongoing incantation/séance.
 * Bug Fix #7: added droppedItemId to track the cursed body item entity on the ground.
 */
public class IncantationState {
    private final UUID incantatorUuid;
    private final UUID armorStandEntityId;
    /** Bug Fix #7: the dropped Item entity UUID of the cursed body on the ground */
    UUID droppedItemId;
    private int ticksRemaining;
    private boolean complete;

    public IncantationState(UUID incantatorUuid, UUID armorStandEntityId, int ticks) {
        this.incantatorUuid = incantatorUuid;
        this.armorStandEntityId = armorStandEntityId;
        this.ticksRemaining = ticks;
        this.complete = false;
    }

    public UUID getIncantatorUuid() { return incantatorUuid; }
    public UUID getArmorStandEntityId() { return armorStandEntityId; }
    public UUID getDroppedItemId() { return droppedItemId; }
    public void setDroppedItemId(UUID id) { this.droppedItemId = id; }
    public int getTicksRemaining() { return ticksRemaining; }
    public void setTicksRemaining(int t) { this.ticksRemaining = t; }
    public boolean isComplete() { return complete; }
    public void setComplete(boolean complete) { this.complete = complete; }
    public void tick() { if (ticksRemaining > 0) ticksRemaining--; }
}
