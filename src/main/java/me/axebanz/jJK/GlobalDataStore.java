package me.axebanz.jJK;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * Stores server-wide UNIQUE technique owners.
 * Saved at: plugins/JJKCursedTools/global.yml
 */
public final class GlobalDataStore {

    private final JJKCursedToolsPlugin plugin;
    private final File file;
    private YamlConfiguration y;

    public GlobalDataStore(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "global.yml");
        reload();
    }

    public void reload() {
        if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
        this.y = YamlConfiguration.loadConfiguration(file);
    }

    public void save() {
        try {
            y.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed saving global.yml: " + e.getMessage());
        }
    }

    public UUID copyOwner() {
        String s = y.getString("unique.copy.owner", null);
        if (s == null || s.isBlank()) return null;
        try { return UUID.fromString(s); }
        catch (Exception ex) { return null; }
    }

    public void setCopyOwner(UUID uuid) {
        y.set("unique.copy.owner", uuid == null ? null : uuid.toString());
        save();
    }
}