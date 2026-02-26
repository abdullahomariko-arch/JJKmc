package me.axebanz.jJK;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class CursedToolFactory {
    private static final Map<String, ItemCreator> creators = new HashMap<>();

    static {
        creators.put("split_soul_katana", amount -> createNamed(Material.NETHERITE_SWORD, "§5§lSplit Soul Katana", amount, "§7Cleaves the soul itself."));
        creators.put("cursed_blade", amount -> createNamed(Material.IRON_SWORD, "§5Cursed Blade", amount, "§7Imbued with cursed energy."));
        creators.put("straw_doll_hammer", amount -> createNamed(Material.WOODEN_HOE, "§6Straw Doll Hammer", amount, "§7Strike to bind cursed energy."));
        creators.put("straw_doll_nail", amount -> createNamed(Material.STICK, "§6Straw Doll Nail", amount, "§7Pierce to channel resonance."));
        creators.put("cursed_body", amount -> CursedBodyItem.create());
        creators.put("binding_vow_scroll", amount -> createNamed(Material.PAPER, "§5Binding Vow Scroll", amount, "§7A contract of cursed energy."));
        creators.put("cursed_tool", amount -> createNamed(Material.DIAMOND_SWORD, "§aCursed Tool", amount, "§7A tool of cursed energy."));
        creators.put("domain_scroll", amount -> createNamed(Material.BOOK, "§5§lDomain Scroll", amount, "§7Opens a domain expansion."));
    }

    public ItemStack createItem(String id, int amount) {
        ItemCreator creator = creators.get(id.toLowerCase());
        if (creator == null) return null;
        return creator.create(amount);
    }

    private static ItemStack createNamed(Material mat, String name, int amount, String lore) {
        ItemStack item = new ItemStack(mat, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    @FunctionalInterface
    private interface ItemCreator {
        ItemStack create(int amount);
    }
}
