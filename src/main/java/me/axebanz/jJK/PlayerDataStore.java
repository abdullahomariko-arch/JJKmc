package me.axebanz.jJK;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerDataStore {
    private final JJKCursedToolsPlugin plugin;
    private final Map<UUID, PlayerProfile> profiles = new HashMap<>();
    private File dataFolder;

    public PlayerDataStore(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");
        if (!dataFolder.exists()) dataFolder.mkdirs();
    }

    public PlayerProfile getOrCreate(UUID uuid) {
        return profiles.computeIfAbsent(uuid, id -> loadOrCreate(id));
    }

    private PlayerProfile loadOrCreate(UUID uuid) {
        File f = new File(dataFolder, uuid + ".yml");
        if (f.exists()) {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
            PlayerProfile p = new PlayerProfile(uuid);
            p.setTechnique(cfg.getString("technique", null));
            p.setPermaDead(cfg.getBoolean("permadead", false));
            p.setCursedEnergy(cfg.getInt("ce", 100));
            return p;
        }
        return new PlayerProfile(uuid);
    }

    public void save(UUID uuid) {
        PlayerProfile p = profiles.get(uuid);
        if (p == null) return;
        File f = new File(dataFolder, uuid + ".yml");
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("technique", p.getTechnique());
        cfg.set("permadead", p.isPermaDead());
        cfg.set("ce", p.getCursedEnergy());
        try {
            cfg.save(f);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save data for " + uuid + ": " + e.getMessage());
        }
    }

    public void unload(UUID uuid) {
        save(uuid);
        profiles.remove(uuid);
    }
}
