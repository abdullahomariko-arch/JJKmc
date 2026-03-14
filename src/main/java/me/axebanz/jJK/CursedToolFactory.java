package me.axebanz.jJK;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public final class CursedToolFactory {

    private final JJKCursedToolsPlugin plugin;
    private final ItemIds ids;

    public CursedToolFactory(JJKCursedToolsPlugin plugin, ItemIds ids) {
        this.plugin = plugin;
        this.ids = ids;
    }

    public ItemStack create(ToolId tool, int amount) {
        ConfigurationSection sec = plugin.cfg().c().getConfigurationSection("tools." + tool.id);
        if (sec == null) {
            return new ItemStack(Material.STICK, Math.max(1, amount));
        }

        Material mat = Material.matchMaterial(sec.getString("material", "NETHERITE_SWORD"));
        if (mat == null) mat = Material.NETHERITE_SWORD;

        ItemStack it = new ItemStack(mat, Math.max(1, amount));
        ItemMeta meta = it.getItemMeta();

        meta.setDisplayName(sec.getString("displayName", tool.id));

        int cmd = sec.getInt("customModelData", 0);
        if (cmd > 0) meta.setCustomModelData(cmd);

        boolean unbreakable = sec.getBoolean("unbreakable", false);
        if (unbreakable) {
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        }

        List<String> lore = new ArrayList<>();
        lore.add("§7Cursed Tool");
        lore.add("§8ID: §f" + tool.id);
        meta.setLore(lore);

        meta.getPersistentDataContainer().set(ids.TOOL_ID, PersistentDataType.STRING, tool.id);
        meta.getPersistentDataContainer().set(ids.TOOL_VERSION, PersistentDataType.INTEGER, 1);

        it.setItemMeta(meta);
        return it;
    }

    public ToolId identify(ItemStack it) {
        if (it == null || !it.hasItemMeta()) return null;
        ItemMeta meta = it.getItemMeta();
        String id = meta.getPersistentDataContainer().get(ids.TOOL_ID, PersistentDataType.STRING);
        return ToolId.from(id);
    }
}
