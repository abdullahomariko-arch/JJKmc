package me.axebanz.jJK;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager {
    private final Map<String, Long> cooldowns = new HashMap<>();

    private String key(UUID uuid, String action) {
        return uuid + ":" + action;
    }

    public boolean isOnCooldown(UUID uuid, String action) {
        String k = key(uuid, action);
        Long end = cooldowns.get(k);
        if (end == null) return false;
        if (System.currentTimeMillis() >= end) {
            cooldowns.remove(k);
            return false;
        }
        return true;
    }

    public void setCooldown(UUID uuid, String action, long millis) {
        cooldowns.put(key(uuid, action), System.currentTimeMillis() + millis);
    }

    public long getRemaining(UUID uuid, String action) {
        String k = key(uuid, action);
        Long end = cooldowns.get(k);
        if (end == null) return 0L;
        long rem = end - System.currentTimeMillis();
        return Math.max(0L, rem);
    }

    public void clearCooldown(UUID uuid, String action) {
        cooldowns.remove(key(uuid, action));
    }
}
