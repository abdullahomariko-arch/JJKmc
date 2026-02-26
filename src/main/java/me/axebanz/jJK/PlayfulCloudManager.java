package me.axebanz.jJK;

import org.bukkit.entity.Player;

public class PlayfulCloudManager {
    private final JJKCursedToolsPlugin plugin;

    public PlayfulCloudManager(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    public void activate(Player player) {
        player.sendMessage(plugin.cfg().prefix() + "§bPlayful Cloud activated!");
    }
}
