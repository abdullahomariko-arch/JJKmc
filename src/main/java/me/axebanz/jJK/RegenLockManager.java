package me.axebanz.jJK;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RegenLockManager {
    private final Map<UUID, Long> regenLocks = new HashMap<>();

    public void lockRegen(UUID uuid, long durationMillis) {
        regenLocks.put(uuid, System.currentTimeMillis() + durationMillis);
    }

    public boolean isRegenLocked(UUID uuid) {
        Long lockEnd = regenLocks.get(uuid);
        if (lockEnd == null) return false;
        if (System.currentTimeMillis() >= lockEnd) {
            regenLocks.remove(uuid);
            return false;
        }
        return true;
    }

    public void unlockRegen(UUID uuid) {
        regenLocks.remove(uuid);
    }
}
