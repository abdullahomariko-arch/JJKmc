package me.axebanz.jJK;

import org.bukkit.entity.Player;

import java.util.UUID;

public final class NullifyManager {

    private final JJKCursedToolsPlugin plugin;
    private final TechniqueManager techniqueManager;
    private final PlayerDataStore store;

    public NullifyManager(JJKCursedToolsPlugin plugin, TechniqueManager techniqueManager, PlayerDataStore store) {
        this.plugin = plugin;
        this.techniqueManager = techniqueManager;
        this.store = store;
    }

    public boolean isNullified(UUID uuid) {
        return store.get(uuid).nullifiedUntilMs > System.currentTimeMillis();
    }

    public long remainingSeconds(UUID uuid) {
        long rem = Math.max(0, store.get(uuid).nullifiedUntilMs - System.currentTimeMillis());
        return rem / 1000L;
    }

    public void applyNullify(Player target, Player attacker, int seconds) {
        long until = System.currentTimeMillis() + seconds * 1000L;
        PlayerProfile tp = store.get(target.getUniqueId());
        tp.nullifiedUntilMs = until;

        // reapply window stored for attacker UI
        PlayerProfile ap = store.get(attacker.getUniqueId());
        ap.isohReapplyUntilMs = until;
        ap.isohReapplyTargetUuid = target.getUniqueId().toString();

        store.save(target.getUniqueId());
        store.save(attacker.getUniqueId());
    }

    public String targetTechniqueColor(UUID targetUuid) {
        return techniqueManager.techniqueColorHex(targetUuid);
    }

    public String targetTechniqueName(UUID targetUuid) {
        return techniqueManager.techniqueName(targetUuid);
    }
}
