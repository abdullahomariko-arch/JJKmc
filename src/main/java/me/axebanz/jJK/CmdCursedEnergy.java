package me.axebanz.jJK;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * /jjk ce <check|set|add> — Cursed Energy progression commands.
 */
public final class CmdCursedEnergy implements SubCommand {

    private final JJKCursedToolsPlugin plugin;

    public CmdCursedEnergy(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public String name() { return "ce"; }
    @Override public String permission() { return ""; } // check is public; set/add check separately

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        String pref = plugin.cfg().prefix();
        // args[0] = "ce", args[1] = subcommand
        if (args.length < 2) {
            return showHelp(sender);
        }

        switch (args[1].toLowerCase()) {
            case "check" -> {
                Player target;
                if (args.length >= 3) {
                    if (!sender.hasPermission("jjk.ce.check.others")) {
                        sender.sendMessage(pref + "§cNo permission to check others.");
                        return true;
                    }
                    target = Bukkit.getPlayer(args[2]);
                    if (target == null) {
                        sender.sendMessage(pref + "§cPlayer not found.");
                        return true;
                    }
                } else {
                    if (!(sender instanceof Player p)) {
                        sender.sendMessage(pref + "§cSpecify a player name.");
                        return true;
                    }
                    target = p;
                }
                int level = plugin.ce().getCeLevel(target.getUniqueId());
                int maxLevel = plugin.ce().getMaxCeLevel(target.getUniqueId());
                int xp = plugin.ce().getCeLevelXp(target.getUniqueId());
                int maxXp = plugin.ce().getMaxCeLevelXp(target.getUniqueId());
                boolean sixEyes = plugin.sixEyes() != null && plugin.sixEyes().hasSixEyes(target);
                String name = target.getName();
                sender.sendMessage(pref + "§eCE Level for §f" + name + "§e:");
                sender.sendMessage("  §7Level: §f" + level + " §8/ §f" + maxLevel
                        + (sixEyes ? " §b[Six Eyes]" : ""));
                sender.sendMessage("  §7XP: §f" + xp + " §8/ §f" + maxXp);
                sender.sendMessage("  §7RCT unlocked: " + (plugin.ce().hasRct(target.getUniqueId()) ? "§aYes" : "§cNo"));
                sender.sendMessage("  §7Fall damage reduction: " + (plugin.ce().hasFallDamageReduction(target.getUniqueId()) ? "§aYes (40%)" : "§cNo"));
            }
            case "set" -> {
                if (!sender.hasPermission("jjk.ce.admin")) {
                    sender.sendMessage(pref + "§cNo permission.");
                    return true;
                }
                if (args.length < 4) {
                    sender.sendMessage(pref + "§cUsage: /jjk ce set <player> <level>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[2]);
                if (target == null) {
                    sender.sendMessage(pref + "§cPlayer not found.");
                    return true;
                }
                int level;
                try { level = Integer.parseInt(args[3]); }
                catch (NumberFormatException ex) {
                    sender.sendMessage(pref + "§cInvalid number.");
                    return true;
                }
                int xp = level * CursedEnergyManager.XP_PER_LEVEL;
                plugin.ce().setCeLevelXp(target.getUniqueId(), xp);
                sender.sendMessage(pref + "§aSet CE level for §f" + target.getName() + "§a to §f" + level + "§a.");
            }
            case "add" -> {
                if (!sender.hasPermission("jjk.ce.admin")) {
                    sender.sendMessage(pref + "§cNo permission.");
                    return true;
                }
                if (args.length < 4) {
                    sender.sendMessage(pref + "§cUsage: /jjk ce add <player> <amount>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[2]);
                if (target == null) {
                    sender.sendMessage(pref + "§cPlayer not found.");
                    return true;
                }
                int amount;
                try { amount = Integer.parseInt(args[3]); }
                catch (NumberFormatException ex) {
                    sender.sendMessage(pref + "§cInvalid number.");
                    return true;
                }
                int xp = amount * CursedEnergyManager.XP_PER_LEVEL;
                plugin.ce().addCeLevelXp(target.getUniqueId(), xp);
                int newLevel = plugin.ce().getCeLevel(target.getUniqueId());
                sender.sendMessage(pref + "§aAdded §f" + amount + "§a CE levels to §f" + target.getName()
                        + "§a. Now at §f" + newLevel + "§a.");
            }
            default -> { return showHelp(sender); }
        }
        return true;
    }

    private boolean showHelp(CommandSender sender) {
        String pref = plugin.cfg().prefix();
        sender.sendMessage(pref + "§eCursed Energy Progression:");
        sender.sendMessage("  §f/jjk ce check §7[player] — Show CE level");
        sender.sendMessage("  §f/jjk ce set <player> <level> §7— §cAdmin: set CE level");
        sender.sendMessage("  §f/jjk ce add <player> <amount> §7— §cAdmin: add CE levels");
        return true;
    }

    @Override
    public List<String> tab(CommandSender sender, String[] args) {
        if (args.length == 1) return List.of("check", "set", "add");
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("set") || sub.equals("add") || sub.equals("check")) {
                List<String> names = new ArrayList<>();
                Bukkit.getOnlinePlayers().forEach(p -> names.add(p.getName()));
                return names;
            }
        }
        if (args.length == 3) {
            String sub = args[0].toLowerCase();
            if (sub.equals("set")) return List.of("0", "20", "50", "100", "200");
            if (sub.equals("add")) return List.of("5", "10", "20", "50");
        }
        return List.of();
    }
}
