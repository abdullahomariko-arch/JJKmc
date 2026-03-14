package me.axebanz.jJK;

import java.util.UUID;

public final class CooldownManager {

    private final JJKCursedToolsPlugin plugin;
    private final PlayerDataStore store;

    public CooldownManager(JJKCursedToolsPlugin plugin, PlayerDataStore store) {
        this.plugin = plugin;
        this.store = store;
    }

    public boolean isOnCooldown(UUID uuid, String key) {
        long now = System.currentTimeMillis();
        long end = store.get(uuid).cooldowns.getOrDefault(key, 0L);
        return end > now;
    }

    public long remainingSeconds(UUID uuid, String key) {
        long now = System.currentTimeMillis();
        long end = store.get(uuid).cooldowns.getOrDefault(key, 0L);
        long remMs = Math.max(0, end - now);
        return remMs / 1000L;
    }

    public void setCooldown(UUID uuid, String key, long seconds) {
        long end = System.currentTimeMillis() + (seconds * 1000L);
        store.get(uuid).cooldowns.put(key, end);
        store.save(uuid);
    }

    public void clearAll(UUID uuid) {
        store.get(uuid).cooldowns.clear();
        store.save(uuid);
    }

    public void onDeath(UUID uuid) {
        if (!plugin.cfg().persistCooldownsOnDeath()) {
            clearAll(uuid);
        }
    }
}
