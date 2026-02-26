package me.axebanz.jJK;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public class CmdPermadeath implements CommandExecutor, TabCompleter {
    private final JJKCursedToolsPlugin plugin;
    private final PlayerDataStore dataStore;

    public CmdPermadeath(JJKCursedToolsPlugin plugin, PlayerDataStore dataStore) {
        this.plugin = plugin;
        this.dataStore = dataStore;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("jjk.admin")) {
            sender.sendMessage(plugin.cfg().prefix() + "§cNo permission.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(plugin.cfg().prefix() + "§cUsage: /" + label + " <set|clear> <player>");
            return true;
        }
        String action = args[0].toLowerCase();
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.cfg().prefix() + "§cPlayer not found: " + args[1]);
            return true;
        }
        PlayerProfile profile = dataStore.getOrCreate(target.getUniqueId());
        switch (action) {
            case "set" -> {
                profile.setPermaDead(true);
                dataStore.save(target.getUniqueId());
                sender.sendMessage(plugin.cfg().prefix() + "§aPermadeath set for " + target.getName());
            }
            case "clear" -> {
                profile.setPermaDead(false);
                dataStore.save(target.getUniqueId());
                sender.sendMessage(plugin.cfg().prefix() + "§aPermadeath cleared for " + target.getName());
            }
            default -> sender.sendMessage(plugin.cfg().prefix() + "§cUsage: /" + label + " <set|clear> <player>");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) return List.of("set", "clear");
        return List.of();
    }
}
