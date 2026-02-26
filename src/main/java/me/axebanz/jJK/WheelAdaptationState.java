package me.axebanz.jJK;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WheelAdaptationState {
    private final UUID uuid;
    private int combatScore;
    private int adaptations;

    public WheelAdaptationState(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() { return uuid; }
    public int getCombatScore() { return combatScore; }
    public void setCombatScore(int score) { this.combatScore = score; }
    public int getAdaptations() { return adaptations; }
    public void setAdaptations(int adaptations) { this.adaptations = adaptations; }
    public void addCombatScore(int amount) { combatScore += amount; }
}
