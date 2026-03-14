package me.axebanz.jJK;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RikaStorageGUI implements Listener {

    private static final int SIZE = 27;
    private static final String TITLE = "§5Rika Storage";

    private final JJKCursedToolsPlugin plugin;

    // open inventories by owner
    private final Map<UUID, Inventory> open = new ConcurrentHashMap<>();

    public RikaStorageGUI(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player owner) {
        UUID u = owner.getUniqueId();
        PlayerProfile prof = plugin.data().get(u);

        Inventory inv = RikaStorageSerializer.fromBase64(prof.rikaStorageBase64, SIZE, TITLE);
        open.put(u, inv);

        owner.openInventory(inv);
        owner.playSound(owner.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 0.8f, 1.2f);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player p)) return;

        UUID u = p.getUniqueId();
        Inventory inv = open.get(u);
        if (inv == null) return;

        // only save if it's OUR inventory instance
        if (e.getInventory() != inv) return;

        PlayerProfile prof = plugin.data().get(u);
        prof.rikaStorageBase64 = RikaStorageSerializer.toBase64(inv);
        plugin.data().save(u);

        open.remove(u);
        Bukkit.getScheduler().runTaskLater(plugin, () ->
                p.playSound(p.getLocation(), Sound.BLOCK_ENDER_CHEST_CLOSE, 0.8f, 1.2f), 1L);
    }
}