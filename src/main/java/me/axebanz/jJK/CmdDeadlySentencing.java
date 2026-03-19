package me.axebanz.jJK;

import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

/**
 * /deadlysentencing <barrage|slam|domain|status>
 */
public final class CmdDeadlySentencing implements CommandExecutor, TabCompleter {

    private final JJKCursedToolsPlugin plugin;

    public CmdDeadlySentencing(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(plugin.cfg().prefix() + "§cPlayers only.");
            return true;
        }

        String assignedId = plugin.techniqueManager().getAssignedId(p.getUniqueId());
        if (!"deadly_sentencing".equalsIgnoreCase(assignedId)) {
            p.sendMessage(plugin.cfg().prefix() + "§cYou don't have §6Deadly Sentencing §cequipped.");
            return true;
        }

        DeadlySentencingManager mgr = plugin.deadlySentencing();
        if (mgr == null) {
            p.sendMessage(plugin.cfg().prefix() + "§cDeadly Sentencing system not loaded.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(p, label);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "barrage" -> mgr.activateHammerBarrage(p);
            case "slam" -> mgr.startHammerSlamCharge(p);
            case "domain", "expand" -> mgr.expandDomain(p);
            case "status" -> {
                p.sendMessage(plugin.cfg().prefix() + "§6Deadly Sentencing Status:");
                boolean barrageCD = plugin.cooldowns().isOnCooldown(p.getUniqueId(), "ds_barrage");
                boolean slamCD = plugin.cooldowns().isOnCooldown(p.getUniqueId(), "ds_slam");
                boolean domainCD = plugin.cooldowns().isOnCooldown(p.getUniqueId(), "domain_deadly_sentencing");
                p.sendMessage("  §7Barrage: " + (barrageCD ? "§cOn cooldown (" + plugin.cooldowns().remainingSeconds(p.getUniqueId(), "ds_barrage") + "s)" : "§aReady"));
                p.sendMessage("  §7Hammer Slam: " + (slamCD ? "§cOn cooldown (" + plugin.cooldowns().remainingSeconds(p.getUniqueId(), "ds_slam") + "s)" : "§aReady"));
                p.sendMessage("  §7Domain: " + (domainCD ? "§cOn cooldown (" + plugin.cooldowns().remainingSeconds(p.getUniqueId(), "domain_deadly_sentencing") + "s)" : "§aReady"));
            }
            default -> sendHelp(p, label);
        }
        return true;
    }

    private void sendHelp(Player p, String label) {
        p.sendMessage(plugin.cfg().prefix() + "§6Deadly Sentencing Technique:");
        p.sendMessage("  §f/" + label + " barrage §7— Hammer Barrage (5 rapid hits)");
        p.sendMessage("  §f/" + label + " slam §7— Big Hammer Slam (charge 2s, massive AOE)");
        p.sendMessage("  §f/" + label + " domain §7— Domain Expansion: Deadly Sentencing");
        p.sendMessage("  §f/" + label + " status §7— Show cooldowns");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!(sender instanceof Player p)) return List.of();
        String assignedId = plugin.techniqueManager().getAssignedId(p.getUniqueId());
        if (!"deadly_sentencing".equalsIgnoreCase(assignedId)) return List.of();
        if (args.length == 1) return List.of("barrage", "slam", "domain", "status");
        return List.of();
    }
}
