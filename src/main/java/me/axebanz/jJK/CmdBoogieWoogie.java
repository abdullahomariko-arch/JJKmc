package me.axebanz.jJK;

import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

public final class CmdBoogieWoogie implements CommandExecutor, TabCompleter {

    private final JJKCursedToolsPlugin plugin;
    private final BoogieWoogieManager boogie;

    public CmdBoogieWoogie(JJKCursedToolsPlugin plugin, BoogieWoogieManager boogie) {
        this.plugin = plugin;
        this.boogie = boogie;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(plugin.cfg().prefix() + "§cPlayers only.");
            return true;
        }

        // FIXED: Technique exclusivity — must have boogie_woogie equipped, not just any technique
        String assignedId = plugin.techniqueManager().getAssignedId(p.getUniqueId());
        if (!"boogie_woogie".equalsIgnoreCase(assignedId)) {
            p.sendMessage(plugin.cfg().prefix() + "§cYou don't have §bBoogie Woogie§c equipped.");
            return true;
        }

        if (args.length == 0) {
            p.sendMessage(plugin.cfg().prefix() + "§cUsage: /" + label + " <clap|swap|clear>");
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "clap" -> boogie.clapSwap(p);
            case "swap" -> boogie.swapMarked(p);
            case "clear" -> boogie.clearMark(p);
            default -> p.sendMessage(plugin.cfg().prefix() + "§cUsage: /" + label + " <clap|swap|clear>");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!(sender instanceof Player p)) return List.of();
        String assignedId = plugin.techniqueManager().getAssignedId(p.getUniqueId());
        if (!"boogie_woogie".equalsIgnoreCase(assignedId)) return List.of();

        if (args.length == 1) return List.of("clap", "swap", "clear");
        return List.of();
    }
}