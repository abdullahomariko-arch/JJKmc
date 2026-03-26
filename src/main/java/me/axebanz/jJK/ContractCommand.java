package me.axebanz.jJK;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public final class ContractCommand implements CommandExecutor, TabCompleter {

    private final JJKCursedToolsPlugin plugin;

    public ContractCommand(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Player only.");
            return true;
        }
        ContractManager mgr = plugin.contracts();
        if (mgr == null) {
            p.sendMessage(plugin.cfg().prefix() + "§cContracts not initialized.");
            return true;
        }
        if (args.length == 0) { sendHelp(p); return true; }

        switch (args[0].toLowerCase()) {
            case "create" -> {
                if (!p.hasPermission("jjk.cursemanipu")) {
                    p.sendMessage(plugin.cfg().prefix() + "§cOnly Curse Manipulation users can create contracts.");
                    return true;
                }
                if (args.length < 3) {
                    p.sendMessage(plugin.cfg().prefix() + "§cUsage: /contract create <playerName> <technique>");
                    p.sendMessage(plugin.cfg().prefix() + "§7Techniques: granite_blast, thin_icebreaker, contractual_contracts");
                    return true;
                }
                mgr.createNewPlayerContract(p, args[1], args[2]);
            }
            default -> sendHelp(p);
        }
        return true;
    }

    private void sendHelp(Player p) {
        p.sendMessage(plugin.cfg().prefix() + "§5Contracts: §7create <playerName> <technique>");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return List.of("create");
        if (args.length == 3 && args[0].equalsIgnoreCase("create"))
            return ContractManager.CONTRACT_TECHNIQUES;
        return List.of();
    }
}
