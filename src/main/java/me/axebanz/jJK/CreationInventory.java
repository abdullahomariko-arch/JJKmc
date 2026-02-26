package me.axebanz.jJK;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.Player;

public class CreationInventory {
    private static final String TITLE = "§dCreation Workshop";
    private static final int SIZE = 54;

    public static void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, SIZE, TITLE);
        player.openInventory(inv);
    }
}
