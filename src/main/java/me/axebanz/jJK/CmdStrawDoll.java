package me.axebanz.jJK;

import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

public final class CmdStrawDoll implements CommandExecutor, TabCompleter {

    private final JJKCursedToolsPlugin plugin;
    private final StrawDollManager manager;

    public CmdStrawDoll(JJKCursedToolsPlugin plugin, StrawDollManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(plugin.cfg().prefix() + "§cPlayers only.");
            return true;
        }

        // Technique exclusivity
        String assignedId = plugin.techniqueManager().getAssignedId(p.getUniqueId());
        if (!"strawdoll".equalsIgnoreCase(assignedId)) {
            p.sendMessage(plugin.cfg().prefix() + "§cYou don't have §6Straw Doll Technique§c equipped.");
            return true;
        }

        if (args.length == 0) {
            p.sendMessage(plugin.cfg().prefix() + "§6Straw Doll Technique:");
            p.sendMessage("§f§7Resonance: Hold hammer + cursed body in offhand, right-click");
            p.sendMessage("§f§7Hairpin: Shoot nail from bow, then /" + label + " hairpin");
            p.sendMessage("§f/" + label + " hairpin §7— Detonate nail lodged in target");
            p.sendMessage("§f/" + label + " bindingvow §7— Activate Straw Doll binding vow");
            p.sendMessage("§f/" + label + " status §7— Show straw doll status");
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "resonance" -> manager.activateResonance(p);
            case "hairpin" -> manager.activateHairpin(p);
            case "bindingvow", "vow" -> {
                if (manager.hasBindingVow(p.getUniqueId())) {
                    p.sendMessage(plugin.cfg().prefix() + "§7Straw Doll Binding Vow is already active.");
                } else {
                    manager.activateBindingVow(p);
                }
            }
            case "status" -> {
                java.util.UUID targetUuid = manager.lastNailHitTarget.get(p.getUniqueId());
                String targetName = "None";
                if (targetUuid != null) {
                    org.bukkit.entity.Entity e = org.bukkit.Bukkit.getEntity(targetUuid);
                    if (e instanceof Player tp) targetName = tp.getName();
                    else if (e != null) targetName = e.getName();
                    else targetName = "Gone";
                }
                p.sendMessage(plugin.cfg().prefix() + "§6Straw Doll Status:");
                p.sendMessage("§7Nail Target: §f" + targetName);
                p.sendMessage("§7Binding Vow: " + (manager.hasBindingVow(p.getUniqueId()) ? "§aActive" : "§7Inactive"));
            }
            default -> p.sendMessage(plugin.cfg().prefix() + "§cUsage: /" + label + " <resonance|hairpin|bindingvow|status>");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!(sender instanceof Player p)) return List.of();
        String assignedId = plugin.techniqueManager().getAssignedId(p.getUniqueId());
        if (!"strawdoll".equalsIgnoreCase(assignedId)) return List.of();
        if (args.length == 1) return List.of("resonance", "hairpin", "bindingvow", "status");
        return List.of();
    }
}