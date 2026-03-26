package me.axebanz.jJK;

import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

public final class CullingGamesCommand implements CommandExecutor, TabCompleter {

    private final JJKCursedToolsPlugin plugin;

    public CullingGamesCommand(JJKCursedToolsPlugin plugin) {
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
            case "enter" -> mgr.registerPlayer(p);
            case "teleport" -> {
                if (args.length < 2) { p.sendMessage(plugin.cfg().prefix() + "§cUsage: /cullinggames teleport <A|B|C|D>"); return true; }
                mgr.teleportToColony(p, args[1]);
            }
            case "addrule" -> {
                if (args.length < 2) { p.sendMessage(plugin.cfg().prefix() + "§cUsage: /cullinggames addrule <text>"); return true; }
                String rule = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                mgr.addRule(p, rule);
            }
            case "info" -> p.sendMessage(plugin.cfg().prefix() + mgr.getColonyInfo(p));
            case "setup" -> {
                if (!p.hasPermission("jjk.admin")) {
                    p.sendMessage(plugin.cfg().prefix() + "§cNo permission.");
                    return true;
                }
                if (args.length < 4) {
                    p.sendMessage(plugin.cfg().prefix() + "§cUsage: /cullinggames setup <A|B|C|D> <x> <z>");
                    return true;
                }
                try {
                    String colonyId = args[1].toUpperCase();
                    int x = Integer.parseInt(args[2]);
                    int z = Integer.parseInt(args[3]);
                    World world = p.getWorld();
                    mgr.setColonyCenter(colonyId, x, z, world);
                    p.sendMessage(plugin.cfg().prefix() + "§eColony §6" + colonyId + "§e center set to " + x + ", " + z + " in " + world.getName());
                } catch (NumberFormatException e) {
                    p.sendMessage(plugin.cfg().prefix() + "§cInvalid coordinates.");
                }
            }
            default -> sendHelp(p);
        }
        return true;
    }

    private void sendHelp(Player p) {
        p.sendMessage(plugin.cfg().prefix() + "§eCulling Games: §7enter | teleport <A-D> | addrule <text> | info | setup <A-D> <x> <z>");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return List.of("enter", "teleport", "addrule", "info", "setup");
        if (args.length == 2 && (args[0].equalsIgnoreCase("teleport") || args[0].equalsIgnoreCase("setup")))
            return List.of("A", "B", "C", "D");
        return List.of();
    }
}
