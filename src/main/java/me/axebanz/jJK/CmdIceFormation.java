package me.axebanz.jJK;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public final class CmdIceFormation implements CommandExecutor, TabCompleter {

    private final JJKCursedToolsPlugin plugin;

    public CmdIceFormation(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Player only.");
            return true;
        }
        IceFormationManager mgr = plugin.iceFormation();
        if (mgr == null) {
            p.sendMessage(plugin.cfg().prefix() + "§cManager not available.");
            return true;
        }
        if (args.length == 0) { sendHelp(p); return true; }

        switch (args[0].toLowerCase()) {
            case "frostcalm"    -> mgr.castFrostCalm(p);
            case "frostcalmmax" -> mgr.castFrostCalmMax(p);
            case "icefall"      -> mgr.castIcefall(p);
            case "status" -> {
                String id = plugin.techniqueManager().getAssignedId(p.getUniqueId());
                if ("ice_formation".equalsIgnoreCase(id)) {
                    p.sendMessage(plugin.cfg().prefix() + "§bIce Formation §7— active.");
                } else {
                    p.sendMessage(plugin.cfg().prefix() + "§cYou don't have Ice Formation equipped.");
                }
            }
            default -> sendHelp(p);
        }
        return true;
    }

    private void sendHelp(Player p) {
        p.sendMessage(plugin.cfg().prefix() + "§bIce Formation: §7frostcalm | frostcalmmax | icefall | status");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return List.of("frostcalm", "frostcalmmax", "icefall", "status");
        return List.of();
    }
}
