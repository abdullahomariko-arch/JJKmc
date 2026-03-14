package me.axebanz.jJK;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a single summoned shikigami (or swarm/pair) in the world.
 */
public final class ShikigamiInstance {

    private final ShikigamiType type;
    private final UUID ownerUuid;
    private UUID entityUuid;
    private UUID secondEntityUuid; // For Divine Dogs pair
    private final List<UUID> swarmEntityUuids = new ArrayList<>();
    private boolean alive = true;
    private long summonedAtMs;

    // Mahoraga adaptation
    private int adaptationStacks = 0;
    private String lastPhenomenonType = null;
    private int consecutiveSameHits = 0;
    private AdaptationTier currentTier = AdaptationTier.RECOGNITION;

    public ShikigamiInstance(ShikigamiType type, UUID ownerUuid) {
        this.type = type;
        this.ownerUuid = ownerUuid;
        this.summonedAtMs = System.currentTimeMillis();
    }

    public ShikigamiType type() { return type; }
    public UUID ownerUuid() { return ownerUuid; }

    public UUID entityUuid() { return entityUuid; }
    public void setEntityUuid(UUID entityUuid) { this.entityUuid = entityUuid; }

    public UUID secondEntityUuid() { return secondEntityUuid; }
    public void setSecondEntityUuid(UUID uuid) { this.secondEntityUuid = uuid; }

    public List<UUID> swarmEntityUuids() { return swarmEntityUuids; }

    public boolean isAlive() { return alive; }
    public void setAlive(boolean alive) { this.alive = alive; }

    public long summonedAtMs() { return summonedAtMs; }

    // Mahoraga adaptation
    public int adaptationStacks() { return adaptationStacks; }
    public void addAdaptationStack() {
        adaptationStacks = Math.min(10, adaptationStacks + 1);
        // Update tier
        int totalHits = adaptationStacks * 100; // rough mapping
        for (AdaptationTier tier : AdaptationTier.values()) {
            if (totalHits >= tier.totalHitsRequired()) {
                currentTier = tier;
            }
        }
    }
    public double adaptationReduction() {
        return currentTier.damageReduction();
    }

    public AdaptationTier currentTier() { return currentTier; }

    public String lastPhenomenonType() { return lastPhenomenonType; }
    public void setLastPhenomenonType(String type) {
        if (type != null && type.equals(lastPhenomenonType)) {
            consecutiveSameHits++;
        } else {
            consecutiveSameHits = 1;
        }
        this.lastPhenomenonType = type;
    }
    public int consecutiveSameHits() { return consecutiveSameHits; }
}