package me.axebanz.jJK;

import org.bukkit.Material;
import java.util.*;

public final class CreationInventory {

    private static final Map<CreationCategory, Material[]> CATEGORY_ITEMS = new HashMap<>();

    static {
        // BLOCKS
        CATEGORY_ITEMS.put(CreationCategory.BLOCKS, new Material[]{
                Material.STONE, Material.DIRT, Material.GRASS_BLOCK, Material.COBBLESTONE,
                Material.OAK_LOG, Material.OAK_LEAVES, Material.OBSIDIAN, Material.SAND,
                Material.GRAVEL, Material.GOLD_ORE, Material.IRON_ORE, Material.COAL_ORE,
                Material.DIAMOND_ORE, Material.EMERALD_ORE, Material.LAPIS_ORE, Material.REDSTONE_ORE,
                Material.CRAFTING_TABLE, Material.FURNACE, Material.CHEST, Material.BARREL,
                Material.BOOKSHELF, Material.MOSSY_COBBLESTONE, Material.OBSIDIAN, Material.MOSSY_STONE_BRICKS,
                Material.BRICKS, Material.SANDSTONE, Material.NETHERRACK, Material.SOUL_SAND,
                Material.WARPED_NYLIUM, Material.CRIMSON_NYLIUM, Material.AMETHYST_BLOCK, Material.DEEPSLATE
        });

        // TOOLS & ARMOUR
        CATEGORY_ITEMS.put(CreationCategory.TOOLS_ARMOUR, new Material[]{
                Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD,
                Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE,
                Material.WOODEN_PICKAXE, Material.STONE_PICKAXE, Material.IRON_PICKAXE, Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE,
                Material.WOODEN_SHOVEL, Material.STONE_SHOVEL, Material.IRON_SHOVEL, Material.DIAMOND_SHOVEL, Material.NETHERITE_SHOVEL,
                Material.WOODEN_HOE, Material.STONE_HOE, Material.IRON_HOE, Material.DIAMOND_HOE, Material.NETHERITE_HOE,
                Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE, Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS,
                Material.CHAINMAIL_HELMET, Material.CHAINMAIL_CHESTPLATE, Material.CHAINMAIL_LEGGINGS, Material.CHAINMAIL_BOOTS,
                Material.IRON_HELMET, Material.IRON_CHESTPLATE, Material.IRON_LEGGINGS, Material.IRON_BOOTS,
                Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS,
                Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE, Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS,
                Material.BOW, Material.CROSSBOW, Material.TRIDENT, Material.SHIELD
        });

        // CONSUMABLES
        CATEGORY_ITEMS.put(CreationCategory.CONSUMABLES, new Material[]{
                Material.APPLE, Material.GOLDEN_APPLE, Material.ENCHANTED_GOLDEN_APPLE,
                Material.BREAD, Material.COOKED_BEEF, Material.COOKED_CHICKEN, Material.COOKED_PORKCHOP, Material.COOKED_MUTTON,
                Material.COOKED_SALMON, Material.COOKED_COD, Material.BEETROOT, Material.CARROT,
                Material.POTATO, Material.BAKED_POTATO, Material.MUSHROOM_STEW, Material.RABBIT_STEW,
                Material.HONEY_BOTTLE, Material.POTION, Material.SPLASH_POTION, Material.LINGERING_POTION,
                Material.MILK_BUCKET, Material.CAKE, Material.COOKIE, Material.MELON_SLICE,
                Material.PUMPKIN_PIE, Material.SWEET_BERRIES, Material.GLOW_BERRIES, Material.CHORUS_FRUIT,
                Material.DRIED_KELP
        });
    }

    public static Material getRandomItem(CreationCategory category) {
        Material[] items = CATEGORY_ITEMS.get(category);
        if (items == null || items.length == 0) return Material.STONE;
        return items[new Random().nextInt(items.length)];
    }

    public static Material[] getAllItems(CreationCategory category) {
        return CATEGORY_ITEMS.getOrDefault(category, new Material[0]);
    }
}