package me.axebanz.jJK;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

public class CmdCopy implements CommandExecutor, TabCompleter {
    private final JJKCursedToolsPlugin plugin;
    private final CopyManager manager;

    public CmdCopy(JJKCursedToolsPlugin plugin, CopyManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(plugin.cfg().prefix() + "§cPlayers only.");
            return true;
        }
        String techId = plugin.techniqueManager().getAssignedId(p.getUniqueId());
        if (!"copy".equalsIgnoreCase(techId)) {
            p.sendMessage(plugin.cfg().prefix() + "§cYou don't have the §aCopy§c technique.");
            return true;
        }
        p.sendMessage(plugin.cfg().prefix() + "§aCopy technique.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        return List.of();
    }
}
