package me.axebanz.jJK;

import java.util.UUID;

public class CreationState {
    private final UUID uuid;
    private int constructCount;
    private boolean workshopOpen;

    public CreationState(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() { return uuid; }
    public int getConstructCount() { return constructCount; }
    public void setConstructCount(int count) { this.constructCount = count; }
    public boolean isWorkshopOpen() { return workshopOpen; }
    public void setWorkshopOpen(boolean open) { this.workshopOpen = open; }
}
