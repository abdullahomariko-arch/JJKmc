package me.axebanz.jJK;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public class CmdReload implements CommandExecutor, TabCompleter {
    private final JJKCursedToolsPlugin plugin;

    public CmdReload(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("jjk.admin")) {
            sender.sendMessage(plugin.cfg().prefix() + "§cNo permission.");
            return true;
        }
        plugin.cfg().reload();
        sender.sendMessage(plugin.cfg().prefix() + "§aConfig reloaded.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        return List.of();
    }
}
