package me.axebanz.jJK;

import java.util.UUID;

public final class RegenLockManager {

    private final JJKCursedToolsPlugin plugin;
    private final PlayerDataStore store;

    public RegenLockManager(JJKCursedToolsPlugin plugin, PlayerDataStore store) {
        this.plugin = plugin;
        this.store = store;
    }

    public void lock(UUID uuid, int seconds) {
        long until = System.currentTimeMillis() + seconds * 1000L;
        store.get(uuid).regenLockedUntilMs = until;
        store.save(uuid);
    }

    public boolean isLocked(UUID uuid) {
        return store.get(uuid).regenLockedUntilMs > System.currentTimeMillis();
    }

    public long remainingSeconds(UUID uuid) {
        long rem = Math.max(0, store.get(uuid).regenLockedUntilMs - System.currentTimeMillis());
        return rem / 1000L;
    }
}
