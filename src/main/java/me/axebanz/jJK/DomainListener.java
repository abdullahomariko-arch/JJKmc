package me.axebanz.jJK;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Iterator;

public final class DomainListener implements Listener {

    private final JJKCursedToolsPlugin plugin;

    public DomainListener(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        Location loc = e.getBlock().getLocation();
        DomainExpansion domain = plugin.domainManager().getDomainAt(loc);
        if (domain != null && domain.isProtectedBlock(loc)) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(plugin.cfg().prefix() + "§cYou cannot break blocks inside a domain!");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent e) {
        Location loc = e.getBlock().getLocation();
        DomainExpansion domain = plugin.domainManager().getDomainAt(loc);
        if (domain != null && domain.isProtectedBlock(loc)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player p = e.getPlayer();
        DomainExpansion domain = plugin.domainManager().getDomain(p);
        if (domain != null) {
            plugin.domainManager().collapse(p);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        DomainExpansion domain = plugin.domainManager().getDomain(p);
        if (domain != null) {
            plugin.domainManager().collapse(p);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onExplosion(EntityExplodeEvent e) {
        Iterator<org.bukkit.block.Block> it = e.blockList().iterator();
        while (it.hasNext()) {
            org.bukkit.block.Block block = it.next();
            Location loc = block.getLocation();
            DomainExpansion domain = plugin.domainManager().getDomainAt(loc);
            if (domain != null && domain.isProtectedBlock(loc)) {
                it.remove();
            }
        }
    }
}