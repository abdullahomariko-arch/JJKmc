package me.axebanz.jJK;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

/**
 * /projection command handler.
 * Bug Fix #3: Checks that the player has "projection" technique assigned.
 */
public class CmdProjection implements CommandExecutor, TabCompleter {
    private final JJKCursedToolsPlugin plugin;
    private final ProjectionManager manager;

    public CmdProjection(JJKCursedToolsPlugin plugin, ProjectionManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(plugin.cfg().prefix() + "§cPlayers only.");
            return true;
        }

        // Bug Fix #3: technique exclusivity
        String techId = plugin.techniqueManager().getAssignedId(p.getUniqueId());
        if (!"projection".equalsIgnoreCase(techId)) {
            p.sendMessage(plugin.cfg().prefix() + "§cYou don't have the §bProjection Sorcery§c technique.");
            return true;
        }

        if (args.length == 0) {
            p.sendMessage(plugin.cfg().prefix() + "§bProjection Sorcery:");
            p.sendMessage("§f/" + label + " program §7— Start programming");
            p.sendMessage("§f/" + label + " dash §7— Execute dash");
            p.sendMessage("§f/" + label + " breakerlunge §7— Breaker Lunge");
            p.sendMessage("§f/" + label + " breakerback §7— Breaker Back");
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "program" -> manager.startProgramming(p);
            case "dash" -> manager.commitProgramming(p, plugin.cfg().projectionDashDistance());
            case "breakerlunge" -> manager.activateBreakerLunge(p);
            case "breakerback" -> manager.activateBreakerBack(p);
            default -> p.sendMessage(plugin.cfg().prefix() + "§cUsage: /" + label + " <program|dash|breakerlunge|breakerback>");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!(sender instanceof Player)) return List.of();
        if (args.length == 1) return List.of("program", "dash", "breakerlunge", "breakerback");
        return List.of();
    }
}
