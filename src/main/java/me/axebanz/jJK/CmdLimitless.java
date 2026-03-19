package me.axebanz.jJK;

import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

/**
 * /limitless <infinity|blue|bluemax|red|redmax|purple|nuke|void|status>
 */
public final class CmdLimitless implements CommandExecutor, TabCompleter {

    private final JJKCursedToolsPlugin plugin;

    public CmdLimitless(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(plugin.cfg().prefix() + "§cPlayers only.");
            return true;
        }

        String assignedId = plugin.techniqueManager().getAssignedId(p.getUniqueId());
        if (!"limitless".equalsIgnoreCase(assignedId)) {
            p.sendMessage(plugin.cfg().prefix() + "§cYou don't have §bLimitless §cequipped.");
            return true;
        }

        LimitlessManager mgr = plugin.limitless();
        if (mgr == null) {
            p.sendMessage(plugin.cfg().prefix() + "§cLimitless system not loaded.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(p, label);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "infinity" -> mgr.toggleInfinity(p);
            case "blue" -> mgr.castBlue(p);
            case "bluemax", "blue_max" -> mgr.castBlueMax(p);
            case "red" -> mgr.castRed(p);
            case "redmax", "red_max" -> mgr.castRedMax(p);
            case "purple" -> mgr.castHollowPurple(p);
            case "nuke" -> mgr.castNuke(p);
            case "void", "domain", "infinitevoid" -> mgr.castInfiniteVoid(p);
            case "status" -> {
                boolean infinity = mgr.isInfinityActive(p);
                boolean canNuke = plugin.data().get(p.getUniqueId()).limitlessCanNuke;
                boolean hasSixEyes = plugin.sixEyes() != null && plugin.sixEyes().hasSixEyes(p);
                int ceLevel = plugin.ce().getCeLevel(p.getUniqueId());
                boolean rct = plugin.ce().hasRct(p.getUniqueId());

                p.sendMessage(plugin.cfg().prefix() + "§bLimitless Status:");
                p.sendMessage("  §7Infinity: " + (infinity ? "§aActive" : "§cInactive"));
                p.sendMessage("  §7Six Eyes: " + (hasSixEyes ? "§aYes" : "§cNo"));
                p.sendMessage("  §7CE Level: §f" + ceLevel + "/" + plugin.ce().getMaxCeLevel(p.getUniqueId()));
                p.sendMessage("  §7RCT (Red unlocked): " + (rct ? "§aYes" : "§cNo §7(need 200 CE)"));
                p.sendMessage("  §7Nuke available: " + (canNuke ? "§aYes" : "§cNo"));
                if (mgr.getStationaryBlueLocation(p.getUniqueId()) != null) {
                    p.sendMessage("  §7Stationary Blue: §aAnchored");
                }
            }
            default -> sendHelp(p, label);
        }
        return true;
    }

    private void sendHelp(Player p, String label) {
        p.sendMessage(plugin.cfg().prefix() + "§bLimitless Technique:");
        p.sendMessage("  §f/" + label + " infinity §7— Toggle Infinity (blocks all attacks)");
        p.sendMessage("  §f/" + label + " blue §7— Pull entities (5-block radius)");
        p.sendMessage("  §f/" + label + " bluemax §7— Max Blue (follows cursor, Shift to anchor)");
        p.sendMessage("  §f/" + label + " red §7— Repel entities [RCT required]");
        p.sendMessage("  §f/" + label + " redmax §7— Max Red (Shift to launch) [RCT required]");
        p.sendMessage("  §f/" + label + " purple §7— Hollow Purple beam [RCT required]");
        p.sendMessage("  §f/" + label + " nuke §7— Hollow Purple Nuke [kill with Max Blue first]");
        p.sendMessage("  §f/" + label + " void §7— Domain Expansion: Infinite Void");
        p.sendMessage("  §f/" + label + " status §7— Show current status");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!(sender instanceof Player p)) return List.of();
        String assignedId = plugin.techniqueManager().getAssignedId(p.getUniqueId());
        if (!"limitless".equalsIgnoreCase(assignedId)) return List.of();
        if (args.length == 1) {
            return List.of("infinity", "blue", "bluemax", "red", "redmax", "purple", "nuke", "void", "status");
        }
        return List.of();
    }
}
