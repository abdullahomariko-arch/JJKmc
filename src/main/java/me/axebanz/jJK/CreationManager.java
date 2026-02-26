package me.axebanz.jJK;

import org.bukkit.entity.Player;

public class CreationManager {
    private final JJKCursedToolsPlugin plugin;

    public CreationManager(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    public void openCreationMenu(Player player) {
        player.sendMessage(plugin.cfg().prefix() + "§dCreation menu coming soon...");
    }

    public void removeConstructs(Player player) {
        player.sendMessage(plugin.cfg().prefix() + "§dConstructs removed.");
    }
}
