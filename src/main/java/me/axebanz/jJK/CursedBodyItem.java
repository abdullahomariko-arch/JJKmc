package me.axebanz.jJK;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class CursedBodyItem {

    private static final int CUSTOM_MODEL_DATA = 90001;

    private final NamespacedKey KEY_BODY;
    private final NamespacedKey KEY_SOURCE;
    private final NamespacedKey KEY_TECHNIQUE;
    private final JJKCursedToolsPlugin plugin;

    public CursedBodyItem(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
        this.KEY_BODY = new NamespacedKey(plugin, "cursed_body");
        this.KEY_SOURCE = new NamespacedKey(plugin, "cursed_body_source");
        this.KEY_TECHNIQUE = new NamespacedKey(plugin, "cursed_body_technique");
    }

    public ItemStack create(UUID sourcePlayer) {
        ItemStack it = new ItemStack(Material.ROTTEN_FLESH);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            String sorcererName = getSorcererName(sourcePlayer);
            String techId = resolveTechniqueId(sourcePlayer);
            String techName = techId != null ? resolveTechniqueName(techId) : "None";

            meta.setDisplayName("§9Cursed Body §8— §f" + sorcererName);
            int cmd = plugin.cfg().c().getInt("tools.cursed_body.customModelData", CUSTOM_MODEL_DATA);
            meta.setCustomModelData(cmd);

            List<String> lore = new ArrayList<>();
            lore.add("§7A fragment of §f" + sorcererName + "§7's body.");
            lore.add("§7Technique: §b" + techName);
            lore.add("");
            lore.add("§8Use on Rika to copy their technique.");
            meta.setLore(lore);

            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.getPersistentDataContainer().set(KEY_BODY, PersistentDataType.INTEGER, 1);
            meta.getPersistentDataContainer().set(KEY_SOURCE, PersistentDataType.STRING, sourcePlayer.toString());
            if (techId != null) {
                meta.getPersistentDataContainer().set(KEY_TECHNIQUE, PersistentDataType.STRING, techId);
            }
            it.setItemMeta(meta);
        }
        return it;
    }

    public boolean isCursedBody(ItemStack it) {
        if (it == null || !it.hasItemMeta()) return false;
        Integer v = it.getItemMeta().getPersistentDataContainer().get(KEY_BODY, PersistentDataType.INTEGER);
        return v != null && v == 1;
    }

    public UUID source(ItemStack it) {
        if (!isCursedBody(it)) return null;
        String s = it.getItemMeta().getPersistentDataContainer().get(KEY_SOURCE, PersistentDataType.STRING);
        if (s == null) return null;
        try { return UUID.fromString(s); } catch (Exception ex) { return null; }
    }

    public String getTechniqueId(ItemStack it) {
        if (!isCursedBody(it)) return null;
        return it.getItemMeta().getPersistentDataContainer().get(KEY_TECHNIQUE, PersistentDataType.STRING);
    }

    private String getSorcererName(UUID uuid) {
        var p = Bukkit.getPlayer(uuid);
        if (p != null) return p.getName();
        var op = Bukkit.getOfflinePlayer(uuid);
        String name = op.getName();
        return name != null ? name : "Unknown Sorcerer";
    }

    /** Returns the effective technique ID for a player, accounting for permadeath state. */
    private String resolveTechniqueId(UUID uuid) {
        plugin.data().load(uuid);
        PlayerProfile prof = plugin.data().get(uuid);
        if (prof.permaDeadTechniqueId != null) return prof.permaDeadTechniqueId;
        return prof.techniqueId;
    }

    private String resolveTechniqueName(String techId) {
        Technique t = plugin.techniques().get(techId);
        return t != null ? t.displayName() : techId;
    }

    /** @deprecated Kept for backwards compatibility — use {@link #getTechniqueId(ItemStack)} for PDC access. */
    @SuppressWarnings("unused")
    private String getSorcererTechniqueId(UUID uuid) {
        return resolveTechniqueId(uuid);
    }
}