package me.axebanz.jJK;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public final class CmdBloodManip implements CommandExecutor, TabCompleter {

    private final JJKCursedToolsPlugin plugin;

    public CmdBloodManip(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Player only.");
            return true;
        }
        BloodManipulationManager mgr = plugin.bloodManip();
        if (mgr == null) {
            p.sendMessage(plugin.cfg().prefix() + "§cManager not available.");
            return true;
        }
        if (args.length == 0) { sendHelp(p); return true; }

        switch (args[0].toLowerCase()) {
            case "supernova" -> mgr.activateSupernova(p);
            case "explode"   -> mgr.explodeSupernova(p);
            case "harpoon"   -> mgr.castHarpoon(p);
            case "piercing"  -> mgr.startPiercingBlood(p);
            case "givewomb"  -> {
                if (!p.hasPermission("jjk.admin")) {
                    p.sendMessage(plugin.cfg().prefix() + "§cNo permission.");
                    return true;
                }
                if (args.length < 2) {
                    p.sendMessage(plugin.cfg().prefix() + "§cUsage: /bloodmanip givewomb <player>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    p.sendMessage(plugin.cfg().prefix() + "§cPlayer not found.");
                    return true;
                }
                mgr.giveWomb(p, target);
            }
            default -> sendHelp(p);
        }
        return true;
    }

    private void sendHelp(Player p) {
        p.sendMessage(plugin.cfg().prefix() + "§4Blood Manipulation: §7supernova | explode | harpoon | piercing | givewomb <player>");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return List.of("supernova", "explode", "harpoon", "piercing", "givewomb");
        return List.of();
    }
}
