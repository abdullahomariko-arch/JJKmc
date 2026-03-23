package me.axebanz.jJK;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class CursedSealItem implements Listener {

    private final JJKCursedToolsPlugin plugin;

    public CursedSealItem(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    public static ItemStack create() {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§4§lCursed Seal");
            meta.setCustomModelData(2002);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static boolean isCursedSeal(ItemStack item) {
        if (item == null || item.getType() != Material.NETHER_STAR) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasCustomModelData() && meta.getCustomModelData() == 2002;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!isCursedSeal(event.getItem())) return;
        Player p = event.getPlayer();
        event.setCancelled(true);
        p.sendMessage(plugin.cfg().prefix() + "§4Cursed Seal activated — Culling Games initialized!");
    }
}
