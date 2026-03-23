package me.axebanz.jJK;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class CursedWombItem {

    public static ItemStack create() {
        ItemStack item = new ItemStack(Material.PAINTING);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§4§lCursed Womb Painting");
            meta.setCustomModelData(2001);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static boolean isCursedWomb(ItemStack item) {
        if (item == null || item.getType() != Material.PAINTING) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasCustomModelData() && meta.getCustomModelData() == 2001;
    }
}
