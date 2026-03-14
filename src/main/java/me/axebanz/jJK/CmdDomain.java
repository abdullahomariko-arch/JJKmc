package me.axebanz.jJK;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

public final class CmdDomain implements CommandExecutor, TabCompleter {

    private final JJKCursedToolsPlugin plugin;

    public CmdDomain(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(plugin.cfg().prefix() + "§cPlayers only.");
            return true;
        }

        String sub = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "expand" -> {
                // Delegate to technique
                Technique tech = plugin.techniqueRegistry().get(
                        plugin.techniqueManager().getAssignedId(p.getUniqueId()));
                if (tech == null) {
                    p.sendMessage(plugin.cfg().prefix() + "§cYou don't have a technique assigned.");
                    return true;
                }
                tech.castAbility(p, AbilitySlot.ONE);
                return true;
            }
            case "collapse" -> {
                DomainExpansion domain = plugin.domainManager().getDomain(p);
                if (domain == null) {
                    p.sendMessage(plugin.cfg().prefix() + "§7You have no active domain.");
                    return true;
                }
                plugin.domainManager().collapse(p);
                p.sendMessage(plugin.cfg().prefix() + "§aDomain collapsed.");
                return true;
            }
            case "status" -> {
                DomainExpansion domain = plugin.domainManager().getDomain(p);
                if (domain == null) {
                    p.sendMessage(plugin.cfg().prefix() + "§7No active domain.");
                } else {
                    p.sendMessage(plugin.cfg().prefix() + "§aDomain: §f" + domain.getName()
                            + " §8| §aRadius: §f" + domain.getRadius()
                            + " §8| §aRefinement: §f" + domain.getRefinement());
                }
                return true;
            }
            default -> {
                p.sendMessage(plugin.cfg().prefix() + "§aDomain Expansion:");
                p.sendMessage("§f/" + label + " expand §7— Expand your domain");
                p.sendMessage("§f/" + label + " collapse §7— Manually collapse domain");
                p.sendMessage("§f/" + label + " status §7— Show domain info");
                return true;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) return List.of("expand", "collapse", "status");
        return List.of();
    }
}
