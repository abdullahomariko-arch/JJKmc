package me.axebanz.jJK;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public class CmdRemoveBindingVow implements CommandExecutor, TabCompleter {
    private final JJKCursedToolsPlugin plugin;
    private final StrawDollManager strawDollManager;

    public CmdRemoveBindingVow(JJKCursedToolsPlugin plugin, StrawDollManager strawDollManager) {
        this.plugin = plugin;
        this.strawDollManager = strawDollManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("jjk.admin")) {
            sender.sendMessage(plugin.cfg().prefix() + "§cNo permission.");
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(plugin.cfg().prefix() + "§cUsage: /" + label + " <player>");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(plugin.cfg().prefix() + "§cPlayer not found: " + args[0]);
            return true;
        }
        strawDollManager.removeBindingVow(target.getUniqueId());
        sender.sendMessage(plugin.cfg().prefix() + "§aBinding vow removed from " + target.getName());
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        return List.of();
    }
}
