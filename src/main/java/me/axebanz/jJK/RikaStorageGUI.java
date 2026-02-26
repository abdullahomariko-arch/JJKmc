package me.axebanz.jJK;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RikaStorageGUI {
    private static final String TITLE = "§5Rika's Storage";
    private static final int SIZE = 27;
    private final Map<UUID, ItemStack[]> storage = new HashMap<>();

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, SIZE, TITLE);
        ItemStack[] contents = storage.getOrDefault(player.getUniqueId(), new ItemStack[SIZE]);
        inv.setContents(contents);
        player.openInventory(inv);
    }

    public void save(Player player, ItemStack[] contents) {
        storage.put(player.getUniqueId(), contents.clone());
    }
}
