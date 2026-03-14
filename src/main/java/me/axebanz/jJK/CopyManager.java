package me.axebanz.jJK;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public final class CopyManager {

    public static final int CE_DEFAULT = 10;

    // Copy owner max CE scaling (ONLY while Copy is equipped)
    public static final int CE_OWNER_NO_RING = 50;
    public static final int CE_OWNER_WITH_RING = 100;

    private final JJKCursedToolsPlugin plugin;

    public CopyManager(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    public UUID ownerUuid() {
        return plugin.global().copyOwner();
    }

    /** ✅ Copy usable ONLY if it's EQUIPPED as the player's current technique */
    public boolean isCopyEquipped(Player p) {
        String assigned = plugin.techniqueManager().getAssignedId(p.getUniqueId());
        return assigned != null && assigned.equalsIgnoreCase("copy");
    }

    /** ✅ Copy usable if Copy technique is equipped — no owner lock */
    public boolean canUseCopy(Player p) {
        return isCopyEquipped(p);
    }

    public boolean hasRingInInventory(Player p) {
        for (ItemStack it : p.getInventory().getContents()) {
            if (plugin.tools().identify(it) == ToolId.YUTA_RING) return true;
        }
        return false;
    }

    /** CE max while Copy is equipped */
    public int ceMaxFor(Player p) {
        if (!canUseCopy(p)) return CE_DEFAULT;
        return hasRingInInventory(p) ? CE_OWNER_WITH_RING : CE_OWNER_NO_RING;
    }

    public int ceMaxFor(UUID u) {
        Player p = Bukkit.getPlayer(u);
        if (p == null) return CE_DEFAULT;
        return ceMaxFor(p);
    }

    /**
     * Enforces:
     * - Unique owner bonding (first ring-holder)
     * - If Copy is NOT equipped => CE behaves like normal
     *   BUT we must NOT clamp players who have OTHER techniques with higher CE (like Projection).
     * - No instant refill; only clamp down and allow regen to fill up
     */
    public void tickPlayer(Player p) {
        UUID u = p.getUniqueId();
        boolean hasRing = hasRingInInventory(p);

        UUID owner = ownerUuid();

        // Unique owner bonding (only once)
        if (owner == null && hasRing) {
            plugin.global().setCopyOwner(u);

            PlayerProfile prof = plugin.data().get(u);
            prof.copyBonded = true;
            prof.techniqueId = "copy"; // force-equip at bond time
            plugin.data().save(u);

            p.sendMessage(plugin.cfg().prefix() + "§dYou have bonded with the Shikigami inside this ring and you have acquired the copy technique.");
            p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.4f);
        }

        PlayerProfile prof = plugin.data().get(u);

        // ✅ If Copy isn't equipped, only manage Copy-specific state.
        // DO NOT touch CE for non-Copy users — other techniques (Projection etc.) manage their own CE max.
        if (!canUseCopy(p)) {
            // Only clamp CE if this player has NO technique that provides a higher max.
            // We use the centralized ce().max() which already accounts for Projection, etc.
            int actualMax = plugin.ce().max(u);
            if (prof.ce > actualMax) prof.ce = actualMax;
            if (prof.ce <= 0) prof.ce = Math.min(CE_DEFAULT, actualMax);
            plugin.data().save(u);
            return;
        }

        // Copy equipped => higher max (50/100)
        int max = ceMaxFor(p);

        // Don't instantly refill, just clamp down
        if (prof.ce > max) prof.ce = max;
        if (prof.ce <= 0) prof.ce = Math.min(CE_DEFAULT, max);

        plugin.data().save(u);
    }
}