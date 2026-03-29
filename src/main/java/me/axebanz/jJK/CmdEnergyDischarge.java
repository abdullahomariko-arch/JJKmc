package me.axebanz.jJK;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public final class CmdEnergyDischarge implements CommandExecutor, TabCompleter {

    private final JJKCursedToolsPlugin plugin;

    public CmdEnergyDischarge(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Player only.");
            return true;
        }
        EnergyDischargeManager mgr = plugin.energyDischarge();
        if (mgr == null) {
            p.sendMessage(plugin.cfg().prefix() + "§cManager not available.");
            return true;
        }
        if (args.length == 0) { sendHelp(p, label); return true; }

        switch (args[0].toLowerCase()) {
            case "tracking" -> mgr.cmdTracking(p);
            case "blast"    -> mgr.cmdBlast(p);
            case "info" -> {
                String id = plugin.techniqueManager().getAssignedId(p.getUniqueId());
                if ("energy_discharge".equalsIgnoreCase(id)) {
                    p.sendMessage(plugin.cfg().prefix() + "§b⚡ Energy Discharge §7— active.");
                    p.sendMessage("§7Passive: §bEnhanced Strikes §7(1.5× melee, 20% reduction)");
                    p.sendMessage("§7Ability 1: §bTracking Beams §7(chargeable, homing)");
                    p.sendMessage("§7Ability 2: §bGranite Blast §7(3-phase ultimate beam)");
                } else {
                    p.sendMessage(plugin.cfg().prefix() + "§cYou don't have §b⚡ Energy Discharge§c equipped.");
                }
            }
            default -> sendHelp(p, label);
        }
        return true;
    }

    private void sendHelp(Player p, String label) {
        p.sendMessage(plugin.cfg().prefix() + "§b⚡ Energy Discharge: §7tracking | blast | info");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return List.of("tracking", "blast", "info");
        return List.of();
    }
}
