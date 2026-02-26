package me.axebanz.jJK;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class CursedBodyItem {
    private static final String CURSED_BODY_KEY = "CURSED_BODY";

    public static ItemStack create() {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§5§lCursed Body");
            meta.setLore(Arrays.asList("§7A body vessel for séance.", "§8[" + CURSED_BODY_KEY + "]"));
            item.setItemMeta(meta);
        }
        return item;
    }

    public static boolean isCursedBody(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return false;
        return meta.getLore() != null && meta.getLore().stream()
                .anyMatch(line -> line.contains(CURSED_BODY_KEY));
    }
}
