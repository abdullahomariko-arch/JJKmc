package me.axebanz.jJK;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

public class CmdBoogieWoogie implements CommandExecutor, TabCompleter {
    private final JJKCursedToolsPlugin plugin;
    private final BoogieWoogieManager manager;

    public CmdBoogieWoogie(JJKCursedToolsPlugin plugin, BoogieWoogieManager manager) {
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
        if (!"boogiewoogie".equalsIgnoreCase(techId)) {
            p.sendMessage(plugin.cfg().prefix() + "§cYou don't have the §eBoogie Woogie§c technique.");
            return true;
        }
        String sub = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "swap";
        switch (sub) {
            case "swap" -> manager.activateSwap(p);
            default -> p.sendMessage(plugin.cfg().prefix() + "§cUsage: /" + label + " <swap>");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) return List.of("swap");
        return List.of();
    }
}
