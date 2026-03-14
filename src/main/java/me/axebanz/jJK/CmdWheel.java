package me.axebanz.jJK;

import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

public final class CmdWheel implements CommandExecutor, TabCompleter {

    private final JJKCursedToolsPlugin plugin;
    private final WheelTierManager wheelTierManager;
    private final WheelUI ui;

    public CmdWheel(JJKCursedToolsPlugin plugin, WheelTierManager wheelTierManager, WheelUI ui) {
        this.plugin = plugin;
        this.wheelTierManager = wheelTierManager;
        this.ui = ui;
    }

    private boolean noPerm(CommandSender sender) {
        if (sender.hasPermission("jjk.wheel")) return false;
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

        switch (sub) {
            case "shuffle" -> {
                wheelTierManager.shuffleMode(p.getUniqueId());
                AdaptationCategory mode = wheelTierManager.getCurrentMode(p.getUniqueId());
                ui.showMode(p, mode, wheelTierManager.getStacks(p.getUniqueId(), mode), 100);
                return true;
            }
            case "set" -> {
                if (args.length < 2) {
                    p.sendMessage(plugin.cfg().prefix() + "§cUsage: /wheel set <mode>");
                    return true;
                }
                AdaptationCategory mode = AdaptationCategory.from(args[1]);
                wheelTierManager.setMode(p.getUniqueId(), mode);
                ui.showMode(p, mode, wheelTierManager.getStacks(p.getUniqueId(), mode), 100);
                return true;
            }
            case "status" -> {
                AdaptationCategory mode = wheelTierManager.getCurrentMode(p.getUniqueId());
                int stacks = wheelTierManager.getStacks(p.getUniqueId(), mode);
                double reduction = wheelTierManager.getDamageReduction(p.getUniqueId());

                p.sendMessage(plugin.cfg().prefix() + "§fWheel Status:");
                p.sendMessage("§7Mode: " + mode.color() + mode.displayName());
                p.sendMessage("§7Stacks: §f" + stacks + "§7/§f100");
                p.sendMessage("§7Damage Reduction: §c" + (int)(reduction * 100) + "%");
                return true;
            }
            case "reset" -> {
                if (!sender.hasPermission("jjk.wheel.admin")) {
                    sender.sendMessage(plugin.cfg().prefix() + "§cNo permission.");
                    return true;
                }
                wheelTierManager.clearState(p.getUniqueId());
                p.sendMessage(plugin.cfg().prefix() + "§aWheel state reset.");
                return true;
            }
            default -> {
                p.sendMessage(plugin.cfg().prefix() + "§cUsage: /wheel <shuffle|set|status|reset>");
                return true;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) return List.of();
        if (!sender.hasPermission("jjk.wheel")) return List.of();

        if (args.length == 1) {
            return List.of("shuffle", "set", "status", "reset");
        }
        if (args.length == 2 && "set".equalsIgnoreCase(args[0])) {
            return List.of("melee", "projectile", "explosion", "fire", "lightning", "technique", "true_damage");
        }
        return List.of();
    }
}