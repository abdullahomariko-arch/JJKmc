package me.axebanz.jJK;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class RikaStorageSerializer {
    private final JJKCursedToolsPlugin plugin;

    public RikaStorageSerializer(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    public ItemStack[] load(UUID uuid) {
        File f = new File(plugin.getDataFolder(), "rika/" + uuid + ".yml");
        if (!f.exists()) return new ItemStack[27];
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
        ItemStack[] items = new ItemStack[27];
        for (int i = 0; i < 27; i++) {
            if (cfg.contains("slot." + i)) {
                items[i] = cfg.getItemStack("slot." + i);
            }
        }
        return items;
    }

    public void save(UUID uuid, ItemStack[] items) {
        File f = new File(plugin.getDataFolder(), "rika/" + uuid + ".yml");
        f.getParentFile().mkdirs();
        YamlConfiguration cfg = new YamlConfiguration();
        for (int i = 0; i < items.length; i++) {
            if (items[i] != null) cfg.set("slot." + i, items[i]);
        }
        try { cfg.save(f); } catch (IOException e) {
            plugin.getLogger().warning("Could not save Rika storage for " + uuid);
        }
    }
}
