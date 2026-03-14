package me.axebanz.jJK;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class CopyLifecycleListener implements Listener {

    private final JJKCursedToolsPlugin plugin;
    private int taskId = -1;

    public CopyLifecycleListener(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (taskId != -1) Bukkit.getScheduler().cancelTask(taskId);

        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                plugin.copy().tickPlayer(p);
            }
        }, 20L, 20L);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        plugin.copy().tickPlayer(e.getPlayer());
    }
}