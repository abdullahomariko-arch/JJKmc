package me.axebanz.jJK;

import java.util.*;

/**
 * Per-player data for the Ten Shadows technique.
 */
public final class TenShadowsProfile {

    public final UUID uuid;

    public final Map<String, ShikigamiState> shikigamiStates = new HashMap<>();

    public String activeSummonId = null;
    public String activeSummonEntityUuid = null;

    public boolean ritualActive = false;
    public String ritualTargetId = null;
    public String ritualEntityUuid = null;

    public long summonCooldownUntilMs = 0L;
    public long ritualCooldownUntilMs = 0L;

    public boolean divineDogWhiteDestroyed = false;
    public boolean divineDogBlackDestroyed = false;

    public boolean nueDestroyed = false;
    public boolean greatSerpentDestroyed = false;

    public int scrollIndex = 0;

    /** ArmorStand-based shikigami health tracking (for Mahoraga etc.) */
    public double armorStandHealth = 0;
    public double armorStandMaxHealth = 0;

    public TenShadowsProfile(UUID uuid) {
        this.uuid = uuid;
        shikigamiStates.put(ShikigamiType.DIVINE_DOGS.id(), ShikigamiState.UNLOCKED);
        for (ShikigamiType type : ShikigamiType.values()) {
            if (type != ShikigamiType.DIVINE_DOGS) {
                shikigamiStates.putIfAbsent(type.id(), ShikigamiState.LOCKED);
            }
        }
    }

    public ShikigamiState getState(ShikigamiType type) {
        return shikigamiStates.getOrDefault(type.id(), ShikigamiState.LOCKED);
    }

    public void setState(ShikigamiType type, ShikigamiState state) {
        shikigamiStates.put(type.id(), state);
    }

    public boolean isUnlocked(ShikigamiType type) {
        ShikigamiState s = getState(type);
        return s == ShikigamiState.UNLOCKED || s == ShikigamiState.FUSED_UNLOCKED;
    }

    public boolean isDestroyed(ShikigamiType type) {
        return getState(type) == ShikigamiState.DESTROYED;
    }

    public boolean isActive(ShikigamiType type) {
        return getState(type) == ShikigamiState.ACTIVE;
    }

    public List<ShikigamiType> getUnlockedShikigami() {
        List<ShikigamiType> list = new ArrayList<>();
        for (ShikigamiType type : ShikigamiType.values()) {
            if (isUnlocked(type)) list.add(type);
        }
        return list;
    }

    public List<ShikigamiType> getLockedShikigami() {
        List<ShikigamiType> list = new ArrayList<>();
        for (ShikigamiType type : ShikigamiType.values()) {
            if (getState(type) == ShikigamiState.LOCKED) list.add(type);
        }
        return list;
    }

    public List<ShikigamiType> getSummonableShikigami() {
        List<ShikigamiType> list = new ArrayList<>();
        for (ShikigamiType type : ShikigamiType.values()) {
            ShikigamiState s = getState(type);
            if (s == ShikigamiState.UNLOCKED || s == ShikigamiState.FUSED_UNLOCKED) {
                list.add(type);
            }
        }
        return list;
    }
}