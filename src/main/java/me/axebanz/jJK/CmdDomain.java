package me.axebanz.jJK;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

public class CmdDomain implements CommandExecutor, TabCompleter {
    private final JJKCursedToolsPlugin plugin;
    private final DomainManager domainManager;

    public CmdDomain(JJKCursedToolsPlugin plugin, DomainManager domainManager) {
        this.plugin = plugin;
        this.domainManager = domainManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(plugin.cfg().prefix() + "§cPlayers only.");
            return true;
        }
        String sub = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "open";
        switch (sub) {
            case "open" -> {
                String techId = plugin.techniqueManager().getAssignedId(p.getUniqueId());
                if (techId == null) {
                    p.sendMessage(plugin.cfg().prefix() + "§cNo technique assigned.");
                    return true;
                }
                // Domain opening is handled by technique managers
                p.sendMessage(plugin.cfg().prefix() + "§5Domain ability triggered.");
            }
            case "close" -> domainManager.closeDomain(p);
            default -> p.sendMessage(plugin.cfg().prefix() + "§cUsage: /" + label + " [open|close]");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) return List.of("open", "close");
        return List.of();
    }
}
