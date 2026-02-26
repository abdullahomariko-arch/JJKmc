package me.axebanz.jJK;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

/**
 * /creation command handler.
 * Bug Fix #3: Checks that the player has "creation" technique assigned.
 */
public class CmdCreation implements CommandExecutor, TabCompleter {
    private final JJKCursedToolsPlugin plugin;
    private final CreationManager manager;

    public CmdCreation(JJKCursedToolsPlugin plugin, CreationManager manager) {
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
        if (!"creation".equalsIgnoreCase(techId)) {
            p.sendMessage(plugin.cfg().prefix() + "§cYou don't have the §dCreation§c technique.");
            return true;
        }

        if (args.length == 0) {
            p.sendMessage(plugin.cfg().prefix() + "§dCreation Technique:");
            p.sendMessage("§f/" + label + " create §7— Open creation menu");
            p.sendMessage("§f/" + label + " remove §7— Remove constructs");
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "create" -> manager.openCreationMenu(p);
            case "remove" -> manager.removeConstructs(p);
            default -> p.sendMessage(plugin.cfg().prefix() + "§cUsage: /" + label + " <create|remove>");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!(sender instanceof Player)) return List.of();
        if (args.length == 1) return List.of("create", "remove");
        return List.of();
    }
}
