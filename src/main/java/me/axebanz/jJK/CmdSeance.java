package me.axebanz.jJK;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public class CmdSeance implements CommandExecutor, TabCompleter {
    private final JJKCursedToolsPlugin plugin;
    private final SeanceManager seanceManager;

    public CmdSeance(JJKCursedToolsPlugin plugin, SeanceManager seanceManager) {
        this.plugin = plugin;
        this.seanceManager = seanceManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(plugin.cfg().prefix() + "§cPlayers only.");
            return true;
        }
        String techId = plugin.techniqueManager().getAssignedId(p.getUniqueId());
        if (!"seance".equalsIgnoreCase(techId)) {
            p.sendMessage(plugin.cfg().prefix() + "§cYou don't have the §5Séance§c technique.");
            return true;
        }
        // Find nearest armor stand with cursed body
        double searchRadius = 10.0;
        org.bukkit.entity.ArmorStand stand = seanceManager.findNearestArmorStandWithBody(p.getLocation(), searchRadius);
        if (stand == null) {
            p.sendMessage(plugin.cfg().prefix() + "§cNo armor stand with a cursed body nearby.");
            return true;
        }
        seanceManager.startIncantation(p, stand);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        return List.of();
    }
}
