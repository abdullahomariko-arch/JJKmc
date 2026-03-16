package me.axebanz.jJK;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shadow Storage — players with Ten Shadows can store items in their shadow.
 *
 * Weight system:
 *   0-9 items  -> No slowness
 *   10-27 items -> Slowness I
 *   28-45 items -> Slowness II
 *   46+ items  -> Slowness III
 *
 * Storage persists across sessions via PlayerProfile (Base64 serialisation,
 * same mechanism used by Rika's storage).
 */
public final class ShadowStorageGUI implements Listener {

    private static final int SIZE = 54;
    private static final String TITLE = "\u00a78Shadow Storage";

    private final JJKCursedToolsPlugin plugin;
    private final Map<UUID, Inventory> open = new ConcurrentHashMap<>();

    public ShadowStorageGUI(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player owner) {
        UUID u = owner.getUniqueId();
        PlayerProfile prof = plugin.data().get(u);

        Inventory inv = RikaStorageSerializer.fromBase64(prof.shadowStorageBase64, SIZE, TITLE);
        open.put(u, inv);

        owner.openInventory(inv);
        owner.playSound(owner.getLocation(), Sound.BLOCK_SOUL_SAND_STEP, 0.8f, 0.6f);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player p)) return;

        UUID u = p.getUniqueId();
        Inventory inv = open.get(u);
        if (inv == null) return;

        if (e.getInventory() != inv) return;

        PlayerProfile prof = plugin.data().get(u);
        prof.shadowStorageBase64 = RikaStorageSerializer.toBase64(inv);
        plugin.data().save(u);

        // Update slowness effect based on stored items
        applyWeightEffect(p, inv);

        open.remove(u);
        Bukkit.getScheduler().runTaskLater(plugin, () ->
                p.playSound(p.getLocation(), Sound.BLOCK_SOUL_SAND_PLACE, 0.8f, 0.6f), 1L);
    }

    /**
     * Apply slowness based on number of items in storage.
     * Called on inventory close.
     */
    public void applyWeightEffect(Player p, Inventory inv) {
        int itemCount = countItems(inv);
        int slowLevel = getSlownessLevel(itemCount);

        p.removePotionEffect(PotionEffectType.SLOWNESS);
        if (slowLevel > 0) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,
                    Integer.MAX_VALUE, slowLevel - 1, false, true, true));
        }
    }

    /**
     * Recalculate weight effect for a player (call on login).
     */
    public void refreshWeightEffect(Player p) {
        UUID u = p.getUniqueId();
        PlayerProfile prof = plugin.data().get(u);
        if (prof.shadowStorageBase64 == null) {
            p.removePotionEffect(PotionEffectType.SLOWNESS);
            return;
        }
        Inventory dummy = RikaStorageSerializer.fromBase64(prof.shadowStorageBase64, SIZE, TITLE);
        applyWeightEffect(p, dummy);
    }

    public static int getSlownessLevel(int itemCount) {
        if (itemCount >= 46) return 3;
        if (itemCount >= 28) return 2;
        if (itemCount >= 10) return 1;
        return 0;
    }

    /** Count the number of occupied inventory slots (not total item amounts). */
    private int countItems(Inventory inv) {
        int count = 0;
        for (ItemStack item : inv.getContents()) {
            if (item != null && !item.getType().isAir()) count++;
        }
        return count;
    }
}
