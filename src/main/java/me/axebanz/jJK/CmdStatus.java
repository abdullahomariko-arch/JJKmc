package me.axebanz.jJK;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public class CmdStatus implements CommandExecutor, TabCompleter {
    private final JJKCursedToolsPlugin plugin;
    private final CursedEnergyManager ceManager;

    public CmdStatus(JJKCursedToolsPlugin plugin, CursedEnergyManager ceManager) {
        this.plugin = plugin;
        this.ceManager = ceManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(plugin.cfg().prefix() + "§cPlayers only.");
            return true;
        }
        String techId = plugin.techniqueManager().getAssignedId(p.getUniqueId());
        int ce = ceManager.get(p.getUniqueId());
        int maxCe = ceManager.getMax();
        p.sendMessage(plugin.cfg().prefix() + "§6Player Status:");
        p.sendMessage("§7Technique: §e" + (techId != null ? techId : "§8None"));
        p.sendMessage("§7Cursed Energy: §b" + ce + "§7/§b" + maxCe);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        return List.of();
    }
}
