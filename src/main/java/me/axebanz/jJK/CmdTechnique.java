package me.axebanz.jJK;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CmdTechnique implements CommandExecutor, TabCompleter {
    private final JJKCursedToolsPlugin plugin;
    private final TechniqueRegistry registry;
    private final TechniqueManager techniqueManager;

    public CmdTechnique(JJKCursedToolsPlugin plugin, TechniqueRegistry registry, TechniqueManager techniqueManager) {
        this.plugin = plugin;
        this.registry = registry;
        this.techniqueManager = techniqueManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p) && !sender.hasPermission("jjk.admin")) {
            sender.sendMessage(plugin.cfg().prefix() + "§cNo permission.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(plugin.cfg().prefix() + "§6Technique commands:");
            sender.sendMessage("§f/" + label + " set <technique> [player] §7— Assign technique");
            sender.sendMessage("§f/" + label + " remove [player] §7— Remove technique");
            sender.sendMessage("§f/" + label + " list §7— List available techniques");
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "set" -> {
                if (args.length < 2) {
                    sender.sendMessage(plugin.cfg().prefix() + "§cUsage: /" + label + " set <technique> [player]");
                    return true;
                }
                Player target = resolveTarget(sender, args, 2);
                if (target == null) return true;
                String techId = args[1].toLowerCase();
                if (!registry.has(techId)) {
                    sender.sendMessage(plugin.cfg().prefix() + "§cUnknown technique: " + techId);
                    return true;
                }
                Technique old = techniqueManager.getAssigned(target.getUniqueId());
                if (old != null) old.onUnequip(target);
                techniqueManager.assign(target.getUniqueId(), techId);
                Technique newTech = registry.get(techId);
                if (newTech != null) newTech.onEquip(target);
                sender.sendMessage(plugin.cfg().prefix() + "§aAssigned §e" + techId + "§a to §e" + target.getName());
            }
            case "remove" -> {
                Player target = resolveTarget(sender, args, 1);
                if (target == null) return true;
                Technique old = techniqueManager.getAssigned(target.getUniqueId());
                if (old != null) old.onUnequip(target);
                techniqueManager.unassign(target.getUniqueId());
                sender.sendMessage(plugin.cfg().prefix() + "§aRemoved technique from §e" + target.getName());
            }
            case "list" -> {
                sender.sendMessage(plugin.cfg().prefix() + "§6Available techniques:");
                for (Technique t : registry.all()) {
                    sender.sendMessage("  §7- §e" + t.getId() + " §7(" + t.getDisplayName() + "§7)");
                }
            }
            default -> sender.sendMessage(plugin.cfg().prefix() + "§cUnknown subcommand: " + sub);
        }
        return true;
    }

    private Player resolveTarget(CommandSender sender, String[] args, int argIndex) {
        if (args.length > argIndex) {
            Player t = org.bukkit.Bukkit.getPlayerExact(args[argIndex]);
            if (t == null) {
                sender.sendMessage(plugin.cfg().prefix() + "§cPlayer not found: " + args[argIndex]);
                return null;
            }
            return t;
        }
        if (sender instanceof Player p) return p;
        sender.sendMessage(plugin.cfg().prefix() + "§cPlease specify a player.");
        return null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) return List.of("set", "remove", "list");
        if (args.length == 2 && "set".equalsIgnoreCase(args[0])) {
            List<String> result = new ArrayList<>();
            for (Technique t : registry.all()) result.add(t.getId());
            return result;
        }
        return List.of();
    }
}
