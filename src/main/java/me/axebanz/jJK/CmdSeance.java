package me.axebanz.jJK;

import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

public final class CmdSeance implements CommandExecutor, TabCompleter {

    private final JJKCursedToolsPlugin plugin;
    private final SeanceManager seanceManager;

    public CmdSeance(JJKCursedToolsPlugin plugin, SeanceManager seanceManager) {
        this.plugin = plugin;
        this.seanceManager = seanceManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(plugin.cfg().prefix() + "§cPlayers only.");
            return true;
        }

        String techId = plugin.techniqueManager().getAssignedId(p.getUniqueId());
        if (!"seance".equalsIgnoreCase(techId)) {
            p.sendMessage(plugin.cfg().prefix() + "§cYou don't have the §5Séance§c technique.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(p, label);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "activate" -> seanceManager.startIncantation(p);
            case "status" -> p.sendMessage(plugin.cfg().prefix() + seanceManager.getStatus(p.getUniqueId()));
            case "cancel" -> seanceManager.cancelSeance(p);
            default -> sendHelp(p, label);
        }

        return true;
    }

    private void sendHelp(Player p, String label) {
        String pref = plugin.cfg().prefix();
        p.sendMessage(pref + "§5/seance activate §7— Start séance near an armor stand");
        p.sendMessage(pref + "§5/seance status §7— Show current séance status");
        p.sendMessage(pref + "§5/seance cancel §7— Cancel active séance");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player p)) return List.of();
        String techId = plugin.techniqueManager().getAssignedId(p.getUniqueId());
        if (!"seance".equalsIgnoreCase(techId)) return List.of();
        if (args.length == 1) return List.of("activate", "status", "cancel");
        return List.of();
    }
}
