package me.axebanz.jJK;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public final class PointsCommand implements CommandExecutor, TabCompleter {

    private final JJKCursedToolsPlugin plugin;

    public PointsCommand(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Player only.");
            return true;
        }
        CullingGamesManager mgr = plugin.cullingGames();
        if (mgr == null) {
            p.sendMessage(plugin.cfg().prefix() + "§cCulling Games not initialized.");
            return true;
        }
        if (args.length == 0) { sendHelp(p); return true; }

        switch (args[0].toLowerCase()) {
            case "give" -> {
                if (args.length < 3) { p.sendMessage(plugin.cfg().prefix() + "§cUsage: /points give <player> <amount>"); return true; }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { p.sendMessage(plugin.cfg().prefix() + "§cPlayer not found."); return true; }
                try {
                    int amount = Integer.parseInt(args[2]);
                    if (amount <= 0) { p.sendMessage(plugin.cfg().prefix() + "§cAmount must be positive."); return true; }
                    mgr.transferPoints(p, target, amount);
                } catch (NumberFormatException e) {
                    p.sendMessage(plugin.cfg().prefix() + "§cInvalid amount.");
                }
            }
            case "check" -> {
                if (args.length >= 2) {
                    Player target = Bukkit.getPlayer(args[1]);
                    if (target == null) { p.sendMessage(plugin.cfg().prefix() + "§cPlayer not found."); return true; }
                    int pts = mgr.getPoints(target.getUniqueId());
                    p.sendMessage(plugin.cfg().prefix() + "§e" + target.getName() + "§7's points: §6" + pts);
                } else {
                    int pts = mgr.getPoints(p.getUniqueId());
                    p.sendMessage(plugin.cfg().prefix() + "§eYour points: §6" + pts);
                }
            }
            default -> sendHelp(p);
        }
        return true;
    }

    private void sendHelp(Player p) {
        p.sendMessage(plugin.cfg().prefix() + "§ePoints: §7give <player> <amount> | check [player]");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return List.of("give", "check");
        return List.of();
    }
}
