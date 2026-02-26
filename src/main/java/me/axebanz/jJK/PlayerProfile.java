package me.axebanz.jJK;

import java.util.UUID;

public class PlayerProfile {
    private final UUID uuid;
    private String technique;
    private boolean permaDead;
    private int cursedEnergy;

    public PlayerProfile(UUID uuid) {
        this.uuid = uuid;
        this.cursedEnergy = 100;
    }

    public UUID getUuid() { return uuid; }
    public String getTechnique() { return technique; }
    public void setTechnique(String technique) { this.technique = technique; }
    public boolean isPermaDead() { return permaDead; }
    public void setPermaDead(boolean permaDead) { this.permaDead = permaDead; }
    public int getCursedEnergy() { return cursedEnergy; }
    public void setCursedEnergy(int cursedEnergy) { this.cursedEnergy = cursedEnergy; }
}
