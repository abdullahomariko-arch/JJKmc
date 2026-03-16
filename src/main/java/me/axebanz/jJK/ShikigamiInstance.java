package me.axebanz.jJK;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

    // Mahoraga wheel adaptation
    private int wheelSpins = 0;
    private long lastWheelSpinMs = 0L;
    private String adaptedPhenomenonType = null;
    private AdaptationTier currentTier = AdaptationTier.RECOGNITION;

    // Legacy adaptation fields (kept for compatibility)
    private int adaptationStacks = 0;
    private String lastPhenomenonType = null;
    private int consecutiveSameHits = 0;

    // Mahoraga attack tracking (Iron Golem based)
    private int attackCycle = 0;
    private long lastAttackMs = 0L;

    // Max Elephant water block tracking
    private final Set<Location> placedWaterBlocks = new HashSet<>();

    // Shikigami-specific cooldowns (nue lightning, piercing ox charge, etc.)
    private long nueLastLightningMs = 0L;
    private long toadLastTongueMs = 0L;
    private long serpentLastGrabMs = 0L;
    private long elephantLastCrushMs = 0L;
    private long oxLastChargeMs = 0L;
    private long deerLastHealMs = 0L;

    // Piercing Ox spawn/charge tracking
    private Location oxSpawnPoint = null;
    private boolean oxCharging = false;

    // Great Serpent grab state
    private UUID serpentGrabbedTarget = null;
    private int serpentShakeTicksLeft = 0;
    private long serpentGrabEndMs = 0L;

    // Nue glide assist
    private long nueLastGlideMs = 0L;

    // Owner attack target tracking (for Toad tongue / Great Serpent grab)
    private UUID toadOwnerTarget = null;
    private UUID serpentOwnerTarget = null;

    // Piercing Ox charge direction (set at summon time)
    private org.bukkit.util.Vector oxChargeDirection = null;

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

    // ---- Mahoraga Wheel Adaptation ----

    public int wheelSpins() { return wheelSpins; }
    public long lastWheelSpinMs() { return lastWheelSpinMs; }
    public String adaptedPhenomenonType() { return adaptedPhenomenonType; }
    public AdaptationTier currentTier() { return currentTier; }

    /**
     * Attempt to spin the wheel for the given phenomenon type.
     * Returns true if the wheel spun (i.e. enough time has passed and spins remain).
     */
    public boolean trySpinWheel(String phenomenonType) {
        long now = System.currentTimeMillis();
        if (wheelSpins >= AdaptationTier.MAX_SPINS) return false;
        if (now - lastWheelSpinMs < AdaptationTier.SPIN_GAP_MS && lastWheelSpinMs > 0) return false;

        wheelSpins = Math.min(AdaptationTier.MAX_SPINS, wheelSpins + 1);
        lastWheelSpinMs = now;
        adaptedPhenomenonType = phenomenonType;
        currentTier = AdaptationTier.fromSpins(wheelSpins);
        // Update legacy fields
        adaptationStacks = wheelSpins;
        return true;
    }

    /** Returns damage multiplier for the given phenomenon (1.0 = full damage, lower = reduced). */
    public double getDamageMult(String phenomenonType) {
        if (adaptedPhenomenonType == null || !adaptedPhenomenonType.equals(phenomenonType)) return 1.0;
        return 1.0 - currentTier.damageReduction();
    }

    public double adaptationReduction() { return currentTier.damageReduction(); }

    // Legacy compatibility
    public int adaptationStacks() { return adaptationStacks; }
    public void addAdaptationStack() {
        adaptationStacks = Math.min(10, adaptationStacks + 1);
        currentTier = AdaptationTier.fromSpins(adaptationStacks);
    }

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

    // ---- Attack cycle ----
    public int attackCycle() { return attackCycle; }
    public void setAttackCycle(int cycle) { this.attackCycle = cycle; }
    public long lastAttackMs() { return lastAttackMs; }
    public void setLastAttackMs(long ms) { this.lastAttackMs = ms; }

    // ---- Max Elephant Water Blocks ----
    public Set<Location> placedWaterBlocks() { return placedWaterBlocks; }

    // ---- Shikigami-specific cooldowns ----
    public long nueLastLightningMs() { return nueLastLightningMs; }
    public void setNueLastLightningMs(long ms) { this.nueLastLightningMs = ms; }

    public long toadLastTongueMs() { return toadLastTongueMs; }
    public void setToadLastTongueMs(long ms) { this.toadLastTongueMs = ms; }

    public long serpentLastGrabMs() { return serpentLastGrabMs; }
    public void setSerpentLastGrabMs(long ms) { this.serpentLastGrabMs = ms; }

    public long elephantLastCrushMs() { return elephantLastCrushMs; }
    public void setElephantLastCrushMs(long ms) { this.elephantLastCrushMs = ms; }

    public long oxLastChargeMs() { return oxLastChargeMs; }
    public void setOxLastChargeMs(long ms) { this.oxLastChargeMs = ms; }

    public long deerLastHealMs() { return deerLastHealMs; }
    public void setDeerLastHealMs(long ms) { this.deerLastHealMs = ms; }

    public Location oxSpawnPoint() { return oxSpawnPoint; }
    public void setOxSpawnPoint(Location loc) { this.oxSpawnPoint = loc; }
    public boolean isOxCharging() { return oxCharging; }
    public void setOxCharging(boolean charging) { this.oxCharging = charging; }

    public UUID serpentGrabbedTarget() { return serpentGrabbedTarget; }
    public void setSerpentGrabbedTarget(UUID uuid) { this.serpentGrabbedTarget = uuid; }
    public int serpentShakeTicksLeft() { return serpentShakeTicksLeft; }
    public void setSerpentShakeTicksLeft(int ticks) { this.serpentShakeTicksLeft = ticks; }
    public void decrementSerpentShakeTicks() { if (serpentShakeTicksLeft > 0) serpentShakeTicksLeft--; }
    public long serpentGrabEndMs() { return serpentGrabEndMs; }
    public void setSerpentGrabEndMs(long ms) { this.serpentGrabEndMs = ms; }

    public long nueLastGlideMs() { return nueLastGlideMs; }
    public void setNueLastGlideMs(long ms) { this.nueLastGlideMs = ms; }

    // Owner attack targets
    public UUID toadOwnerTarget() { return toadOwnerTarget; }
    public void setToadOwnerTarget(UUID uuid) { this.toadOwnerTarget = uuid; }

    public UUID serpentOwnerTarget() { return serpentOwnerTarget; }
    public void setSerpentOwnerTarget(UUID uuid) { this.serpentOwnerTarget = uuid; }

    // Piercing Ox charge direction
    public org.bukkit.util.Vector oxChargeDirection() { return oxChargeDirection; }
    public void setOxChargeDirection(org.bukkit.util.Vector dir) { this.oxChargeDirection = dir; }
}