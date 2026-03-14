package me.axebanz.jJK;

import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

public final class CmdCreation implements CommandExecutor, TabCompleter {

    private final JJKCursedToolsPlugin plugin;
    private final CreationManager creationManager;

    public CmdCreation(JJKCursedToolsPlugin plugin, CreationManager creationManager) {
        this.plugin = plugin;
        this.creationManager = creationManager;
    }

    private boolean noPerm(CommandSender sender) {
        if (sender.hasPermission("jjk.creation")) return false;
        sender.sendMessage(plugin.cfg().prefix() + "§cNo permission.");
        return true;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (noPerm(sender)) return true;
        if (!(sender instanceof Player p)) {
            sender.sendMessage(plugin.cfg().prefix() + "§cPlayers only.");
            return true;
        }

        String sub = (args.length == 0 ? "status" : args[0].toLowerCase(Locale.ROOT));

        // Block technique actions while ISOH-nullified
        // (Allow status/reset; deny shuffle/create)
        if ((sub.equals("shuffle") || sub.equals("create")) && !plugin.techniqueManager().canUseTechniqueActions(p, true)) {
            return true;
        }

        switch (sub) {
            case "shuffle" -> {
                creationManager.shuffle(p);
                return true;
            }
            case "create" -> {
                if (!creationManager.tryCreate(p)) {
                    return true;
                }
                return true;
            }
            case "status" -> {
                CreationCategory cat = creationManager.getCurrentCategory(p.getUniqueId());
                p.sendMessage(plugin.cfg().prefix() + "§fCreation Status:");
                p.sendMessage("§7Current Category: " + cat.color() + cat.displayName());
                p.sendMessage("§7CE Cost: §c" + cat.ceCost());
                p.sendMessage("§7Items Given: §f" + cat.amountGiven());
                return true;
            }
            case "reset" -> {
                if (!sender.hasPermission("jjk.creation.admin")) {
                    sender.sendMessage(plugin.cfg().prefix() + "§cNo permission.");
                    return true;
                }
                creationManager.clearState(p.getUniqueId());
                p.sendMessage(plugin.cfg().prefix() + "§aCreation state reset.");
                return true;
            }
            default -> {
                p.sendMessage(plugin.cfg().prefix() + "§cUsage: /creation <shuffle|create|status|reset>");
                return true;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) return List.of();
        if (!sender.hasPermission("jjk.creation")) return List.of();

        if (args.length == 1) {
            return List.of("shuffle", "create", "status", "reset");
        }
        return List.of();
    }
}