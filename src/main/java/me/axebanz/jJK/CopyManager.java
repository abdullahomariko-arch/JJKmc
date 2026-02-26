package me.axebanz.jJK;

import org.bukkit.entity.Player;

public class CopyManager {
    private final JJKCursedToolsPlugin plugin;

    public CopyManager(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    public void handleCursedBodyDrop(Player victim, Player attacker) {
        // Drop a cursed body item at victim location
        victim.getWorld().dropItemNaturally(victim.getLocation(), CursedBodyItem.create());
        attacker.sendMessage(plugin.cfg().prefix() + "§5Cursed Body dropped!");
    }
}
