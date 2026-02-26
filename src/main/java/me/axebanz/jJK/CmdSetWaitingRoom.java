package me.axebanz.jJK;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public class CmdSetWaitingRoom implements CommandExecutor, TabCompleter {
    private final JJKCursedToolsPlugin plugin;

    public CmdSetWaitingRoom(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("jjk.admin")) {
            sender.sendMessage(plugin.cfg().prefix() + "§cNo permission.");
            return true;
        }
        if (!(sender instanceof Player p)) {
            sender.sendMessage(plugin.cfg().prefix() + "§cPlayers only.");
            return true;
        }
        org.bukkit.Location loc = p.getLocation();
        plugin.getConfig().set("seance.waiting-room.world", loc.getWorld().getName());
        plugin.getConfig().set("seance.waiting-room.x", loc.getX());
        plugin.getConfig().set("seance.waiting-room.y", loc.getY());
        plugin.getConfig().set("seance.waiting-room.z", loc.getZ());
        plugin.saveConfig();
        p.sendMessage(plugin.cfg().prefix() + "§aWaiting room set to your current location.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        return List.of();
    }
}
