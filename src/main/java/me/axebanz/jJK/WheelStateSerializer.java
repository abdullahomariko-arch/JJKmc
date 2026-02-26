package me.axebanz.jJK;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WheelStateSerializer {
    private final JJKCursedToolsPlugin plugin;
    private final WheelTierManager tierManager;

    public WheelStateSerializer(JJKCursedToolsPlugin plugin, WheelTierManager tierManager) {
        this.plugin = plugin;
        this.tierManager = tierManager;
    }

    public void save(UUID uuid) {
        File f = new File(plugin.getDataFolder(), "wheel/" + uuid + ".yml");
        f.getParentFile().mkdirs();
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("tier", tierManager.getTier(uuid));
        try { cfg.save(f); } catch (IOException e) {
            plugin.getLogger().warning("Could not save wheel data for " + uuid);
        }
    }

    public void load(UUID uuid) {
        File f = new File(plugin.getDataFolder(), "wheel/" + uuid + ".yml");
        if (!f.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
        tierManager.setTier(uuid, cfg.getInt("tier", 1));
    }
}
