package me.axebanz.jJK;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

public class CmdWheel implements CommandExecutor, TabCompleter {
    private final JJKCursedToolsPlugin plugin;
    private final WheelTierManager wheelManager;

    public CmdWheel(JJKCursedToolsPlugin plugin, WheelTierManager wheelManager) {
        this.plugin = plugin;
        this.wheelManager = wheelManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(plugin.cfg().prefix() + "§cPlayers only.");
            return true;
        }
        String techId = plugin.techniqueManager().getAssignedId(p.getUniqueId());
        if (!"wheel".equalsIgnoreCase(techId)) {
            p.sendMessage(plugin.cfg().prefix() + "§cYou don't have the §6Wheel§c technique.");
            return true;
        }
        String sub = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "status";
        switch (sub) {
            case "status" -> {
                int tier = wheelManager.getTier(p.getUniqueId());
                p.sendMessage(plugin.cfg().prefix() + "§6Wheel Tier: §e" + tier);
            }
            case "spin" -> wheelManager.spin(p);
            default -> p.sendMessage(plugin.cfg().prefix() + "§cUsage: /" + label + " <status|spin>");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) return List.of("status", "spin");
        return List.of();
    }
}
