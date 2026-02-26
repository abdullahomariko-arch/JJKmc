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
        String techId = plugin.techniqueManager().getAssignedId(p.getUniqueId());
        if (!"strawdoll".equalsIgnoreCase(techId)) {
            p.sendMessage(plugin.cfg().prefix() + "§cYou don't have the §6Straw Doll§c technique.");
            return true;
        }
        if (args.length == 0) {
            p.sendMessage(plugin.cfg().prefix() + "§6Straw Doll Technique:");
            p.sendMessage("§f/" + label + " resonance §7— Activate Resonance");
            p.sendMessage("§f/" + label + " hairpin §7— Detonate Hairpin");
            p.sendMessage("§f/" + label + " status §7— Show status");
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "resonance" -> manager.activateResonance(p);
            case "hairpin" -> manager.activateHairpin(p);
            case "status" -> {
                p.sendMessage(plugin.cfg().prefix() + "§6Straw Doll Status:");
                p.sendMessage("§7Binding Vow: " + (manager.hasBindingVow(p.getUniqueId()) ? "§aActive" : "§7Inactive"));
            }
            default -> p.sendMessage(plugin.cfg().prefix() + "§cUsage: /" + label + " <resonance|hairpin|status>");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!(sender instanceof Player)) return List.of();
        if (args.length == 1) return List.of("resonance", "hairpin", "status");
        return List.of();
    }
}
