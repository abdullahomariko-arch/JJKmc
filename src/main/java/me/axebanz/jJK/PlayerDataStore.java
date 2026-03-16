package me.axebanz.jJK;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerDataStore {

    private final JJKCursedToolsPlugin plugin;
    private final Map<UUID, PlayerProfile> cache = new ConcurrentHashMap<>();

    public PlayerDataStore(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
        ensureFolders();
    }

    private void ensureFolders() {
        File dir = new File(plugin.getDataFolder(), "players");
        if (!dir.exists()) dir.mkdirs();
    }

    public PlayerProfile get(UUID uuid) {
        return cache.computeIfAbsent(uuid, u -> new PlayerProfile(u));
    }

    public void load(UUID uuid) {
        ensureFolders();
        File file = file(uuid);
        PlayerProfile prof = get(uuid);
        if (!file.exists()) return;

        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);

        prof.ce = y.getInt("ce", 10);
        prof.techniqueId = y.getString("technique.id", null);

        prof.nullifiedUntilMs = y.getLong("nullified.untilMs", 0L);

        prof.cooldowns.clear();
        if (y.isConfigurationSection("cooldowns")) {
            for (String k : Objects.requireNonNull(y.getConfigurationSection("cooldowns")).getKeys(false)) {
                long v = y.getLong("cooldowns." + k, 0L);
                if (v > 0) prof.cooldowns.put(k, v);
            }
        }

        prof.regenLockedUntilMs = y.getLong("regenLock.untilMs", 0L);

        prof.isohReapplyUntilMs = y.getLong("isoh.reapplyUntilMs", 0L);
        prof.isohReapplyTargetUuid = y.getString("isoh.reapplyTargetUuid", null);

        prof.wheelMode = y.getString("wheel.mode", "MELEE");
        prof.wheelStacks.clear();
        if (y.isConfigurationSection("wheel.stacks")) {
            for (String k : Objects.requireNonNull(y.getConfigurationSection("wheel.stacks")).getKeys(false)) {
                int v = y.getInt("wheel.stacks." + k, 0);
                if (v > 0) prof.wheelStacks.put(k, v);
            }
        }

        prof.wheelTier = y.getInt("wheel.tier", 0);
        prof.wheelHitCounter = y.getInt("wheel.hitCounter", 0);

        prof.copyBonded = y.getBoolean("copy.bonded", false);
        prof.copiedTechniqueId = y.getString("copy.copiedTechniqueId", null);
        prof.copiedErasedAtMs = y.getLong("copy.copiedErasedAtMs", 0L);

        prof.copyCharges.clear();
        if (y.isConfigurationSection("copy.charges")) {
            for (String k : Objects.requireNonNull(y.getConfigurationSection("copy.charges")).getKeys(false)) {
                int v = y.getInt("copy.charges." + k, 0);
                if (v > 0) prof.copyCharges.put(k, v);
            }
        }

        prof.rikaStorageBase64 = y.getString("copy.rika.storageBase64", null);

        prof.permaDead = y.getBoolean("permadeath.permaDead", false);
        prof.permaDeadTechniqueId = y.getString("permadeath.permaDeadTechniqueId", null);
        prof.seanceBindingVowActive = y.getBoolean("seance.bindingVowActive", false);
        prof.seanceReincarnatedUuid = y.getString("seance.reincarnatedUuid", null);
        prof.isReincarnated = y.getBoolean("seance.isReincarnated", false);

        prof.seanceSpawnWorld = y.getString("seance.spawnWorld", null);
        prof.seanceSpawnX = y.getDouble("seance.spawnX", 0);
        prof.seanceSpawnY = y.getDouble("seance.spawnY", 0);
        prof.seanceSpawnZ = y.getDouble("seance.spawnZ", 0);

        prof.strawDollBindingVowActive = y.getBoolean("strawdoll.bindingVowActive", false);

        // ===== Ten Shadows Shikigami =====
        prof.summonedEntityUuid = y.getString("tenShadows.summonedEntityUuid", null);
        prof.summonedShikigamiId = y.getString("tenShadows.summonedShikigamiId", null);
        prof.ritualShikigamiId = y.getString("tenShadows.ritualShikigamiId", null);

        prof.summonCooldowns.clear();
        if (y.isConfigurationSection("tenShadows.summonCooldowns")) {
            for (String k : Objects.requireNonNull(y.getConfigurationSection("tenShadows.summonCooldowns")).getKeys(false)) {
                long v = y.getLong("tenShadows.summonCooldowns." + k, 0L);
                if (v > 0) prof.summonCooldowns.put(k, v);
            }
        }

        prof.shikigamiStates.clear();
        if (y.isConfigurationSection("tenShadows.shikigamiStates")) {
            for (String k : Objects.requireNonNull(y.getConfigurationSection("tenShadows.shikigamiStates")).getKeys(false)) {
                String stateStr = y.getString("tenShadows.shikigamiStates." + k, "LOCKED");
                try {
                    prof.shikigamiStates.put(k, ShikigamiState.valueOf(stateStr));
                } catch (IllegalArgumentException e) {
                    prof.shikigamiStates.put(k, ShikigamiState.LOCKED);
                }
            }
        }

        prof.shadowStorageBase64 = y.getString("tenShadows.shadowStorageBase64", null);
    }

    public void save(UUID uuid) {
        ensureFolders();
        PlayerProfile prof = get(uuid);
        File file = file(uuid);

        YamlConfiguration y = new YamlConfiguration();
        y.set("ce", prof.ce);

        y.set("technique.id", prof.techniqueId);

        y.set("nullified.untilMs", prof.nullifiedUntilMs);

        for (Map.Entry<String, Long> e : prof.cooldowns.entrySet()) {
            y.set("cooldowns." + e.getKey(), e.getValue());
        }

        y.set("regenLock.untilMs", prof.regenLockedUntilMs);

        y.set("isoh.reapplyUntilMs", prof.isohReapplyUntilMs);
        y.set("isoh.reapplyTargetUuid", prof.isohReapplyTargetUuid);

        y.set("wheel.mode", prof.wheelMode);
        for (Map.Entry<String, Integer> e : prof.wheelStacks.entrySet()) {
            y.set("wheel.stacks." + e.getKey(), e.getValue());
        }

        y.set("wheel.tier", prof.wheelTier);
        y.set("wheel.hitCounter", prof.wheelHitCounter);

        y.set("copy.bonded", prof.copyBonded);
        y.set("copy.copiedTechniqueId", prof.copiedTechniqueId);
        y.set("copy.copiedErasedAtMs", prof.copiedErasedAtMs);

        for (Map.Entry<String, Integer> e : prof.copyCharges.entrySet()) {
            y.set("copy.charges." + e.getKey(), e.getValue());
        }

        y.set("copy.rika.storageBase64", prof.rikaStorageBase64);

        y.set("permadeath.permaDead", prof.permaDead);
        y.set("permadeath.permaDeadTechniqueId", prof.permaDeadTechniqueId);
        y.set("seance.bindingVowActive", prof.seanceBindingVowActive);
        y.set("seance.reincarnatedUuid", prof.seanceReincarnatedUuid);
        y.set("seance.isReincarnated", prof.isReincarnated);

        y.set("seance.spawnWorld", prof.seanceSpawnWorld);
        y.set("seance.spawnX", prof.seanceSpawnX);
        y.set("seance.spawnY", prof.seanceSpawnY);
        y.set("seance.spawnZ", prof.seanceSpawnZ);

        y.set("strawdoll.bindingVowActive", prof.strawDollBindingVowActive);

        // ===== Ten Shadows Shikigami =====
        y.set("tenShadows.summonedEntityUuid", prof.summonedEntityUuid);
        y.set("tenShadows.summonedShikigamiId", prof.summonedShikigamiId);
        y.set("tenShadows.ritualShikigamiId", prof.ritualShikigamiId);

        for (Map.Entry<String, Long> e : prof.summonCooldowns.entrySet()) {
            y.set("tenShadows.summonCooldowns." + e.getKey(), e.getValue());
        }

        for (Map.Entry<String, ShikigamiState> e : prof.shikigamiStates.entrySet()) {
            y.set("tenShadows.shikigamiStates." + e.getKey(), e.getValue().name());
        }

        y.set("tenShadows.shadowStorageBase64", prof.shadowStorageBase64);

        try {
            y.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("Failed saving player data for " + uuid + ": " + ex.getMessage());
        }
    }

    private File file(UUID uuid) {
        return new File(new File(plugin.getDataFolder(), "players"), uuid.toString() + ".yml");
    }
}