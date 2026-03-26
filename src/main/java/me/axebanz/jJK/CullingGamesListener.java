package me.axebanz.jJK;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public final class CullingGamesListener implements Listener {

    private final JJKCursedToolsPlugin plugin;
    private final CullingGamesManager mgr;
    private final KoganeEntity kogane;

    public CullingGamesListener(JJKCursedToolsPlugin plugin, CullingGamesManager mgr, KoganeEntity kogane) {
        this.plugin = plugin;
        this.mgr = mgr;
        this.kogane = kogane;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player p = event.getPlayer();
        // Only check when actually moved to a new block
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) return;

        if (mgr.isNearBarrier(p)) {
            kogane.sendInfo(p, "§eYou are approaching a colony border. Turn back!");
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer != null) {
            // Award points to killer
            mgr.addPoints(killer.getUniqueId(), 100);
            killer.sendMessage(plugin.cfg().prefix() + "§6+100 points §efor sorcerer elimination!");
        }
    }
}
