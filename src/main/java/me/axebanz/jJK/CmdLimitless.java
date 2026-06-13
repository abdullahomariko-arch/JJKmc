package me.axebanz.jJK;

import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

/**
 * /limitless <infinity|blue|red|purple|void|status>
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
            case "red" -> mgr.castRed(p);
            case "purple" -> mgr.castHollowPurple(p);
            case "void", "domain", "infinitevoid" -> mgr.castInfiniteVoid(p);
            case "status" -> {
                boolean infinity = mgr.isInfinityActive(p);
                boolean hasLockedBlue = mgr.hasLockedBlueOrb(p);
                boolean hasSixEyes = plugin.sixEyes() != null && plugin.sixEyes().hasSixEyes(p);
                int ceLevel = plugin.ce().getCeLevel(p.getUniqueId());
                boolean rct = plugin.ce().hasRct(p.getUniqueId());

                p.sendMessage(plugin.cfg().prefix() + "§bLimitless Status:");
                p.sendMessage("  §7Infinity: " + (infinity ? "§aActive" : "§cInactive"));
                p.sendMessage("  §7Six Eyes: " + (hasSixEyes ? "§aYes" : "§cNo"));
                p.sendMessage("  §7CE Level: §f" + ceLevel + "/" + plugin.ce().getMaxCeLevel(p.getUniqueId()));
                p.sendMessage("  §7RCT (Red unlocked): " + (rct ? "§aYes" : "§cNo §7(need 200 CE)"));
                if (mgr.canLockBlue(p)) {
                    p.sendMessage("  §7Blue orb lockable: §aYes — Shift to lock!");
                }
                if (hasLockedBlue) {
                    p.sendMessage("  §7Locked Blue orb: §aAnchored");
                }
            }
            default -> sendHelp(p, label);
        }
        return true;
    }

    private void sendHelp(Player p, String label) {
        p.sendMessage(plugin.cfg().prefix() + "§bLimitless Technique:");
        p.sendMessage("  §f/" + label + " infinity §7— Toggle Infinity (blocks all attacks)");
        p.sendMessage("  §f/" + label + " blue §7— Pull entities (hold key for Maximum Output)");
        p.sendMessage("  §f/" + label + " red §7— Repel entities [RCT required] (hold key for Maximum Output)");
        p.sendMessage("  §f/" + label + " purple §7— Hollow Purple beam [RCT required]");
        p.sendMessage("  §f/" + label + " void §7— Domain Expansion: Infinite Void");
        p.sendMessage("  §f/" + label + " status §7— Show current status");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!(sender instanceof Player p)) return List.of();
        String assignedId = plugin.techniqueManager().getAssignedId(p.getUniqueId());
        if (!"limitless".equalsIgnoreCase(assignedId)) return List.of();
        if (args.length == 1) {
            return List.of("infinity", "blue", "red", "purple", "void", "status");
        }
        return List.of();
    }
}
