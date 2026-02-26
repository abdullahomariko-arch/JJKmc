package me.axebanz.jJK;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

/**
 * Listens for block events in domain expansions.
 * Bug Fix #5: onBlockBreak priority = HIGHEST, ignoreCancelled = false to prevent
 * barrier blocks from ever being broken.
 */
public class DomainListener implements Listener {
    private final JJKCursedToolsPlugin plugin;
    private final DomainManager domainManager;

    public DomainListener(JJKCursedToolsPlugin plugin, DomainManager domainManager) {
        this.plugin = plugin;
        this.domainManager = domainManager;
    }

    // Bug Fix #5: HIGHEST priority, ignoreCancelled = false
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockBreak(BlockBreakEvent event) {
        Location loc = event.getBlock().getLocation();
        // If this block is a barrier block in any active domain, prevent breaking
        if (domainManager.isBarrierBlock(loc)) {
            event.setCancelled(true);
        }
    }
}
