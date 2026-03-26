package me.axebanz.jJK;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.UUID;

public final class PlayerLifecycleListener implements Listener {

    private final JJKCursedToolsPlugin plugin;
    private final PlayerDataStore store;
    private final CursedEnergyManager ce;
    private final BossbarUI bossbar;
    private final ActionbarUI actionbar;

    public PlayerLifecycleListener(JJKCursedToolsPlugin plugin, PlayerDataStore store, CursedEnergyManager ce, BossbarUI bossbar, ActionbarUI actionbar) {
        this.plugin = plugin;
        this.store = store;
        this.ce = ce;
        this.bossbar = bossbar;
        this.actionbar = actionbar;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        store.load(p.getUniqueId());
        ce.ensureInitialized(p.getUniqueId());
        bossbar.attachPlayer(p);
        if (plugin.contracts() != null) {
            plugin.contracts().handleNewPlayerJoin(p);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        actionbar.clear(p.getUniqueId());
        bossbar.detachPlayer(p);
        store.save(p.getUniqueId());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player p = e.getPlayer();
        UUID deadUuid = p.getUniqueId();

        plugin.cooldowns().onDeath(deadUuid);
        DashState.clear(deadUuid);

        // Tell ALL active Rika instances to stop targeting this dead player
        if (plugin.rika() != null) {
            for (Player online : plugin.getServer().getOnlinePlayers()) {
                UUID ownerId = online.getUniqueId();
                if (plugin.rika().isActive(online)) {
                    plugin.rika().onTargetDeath(ownerId, deadUuid);
                }
            }
        }
    }
}