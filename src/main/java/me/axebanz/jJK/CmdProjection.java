package me.axebanz.jJK;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

public final class CmdProjection implements CommandExecutor, TabCompleter {

    private final JJKCursedToolsPlugin plugin;

    public CmdProjection(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(plugin.cfg().prefix() + "§cPlayers only.");
            return true;
        }

        // FIXED: Technique exclusivity — must have projection equipped
        String assignedId = plugin.techniqueManager().getAssignedId(p.getUniqueId());
        if (!"projection".equalsIgnoreCase(assignedId)) {
            p.sendMessage(plugin.cfg().prefix() + "§cYou don't have §5Projection Sorcery§c equipped.");
            return true;
        }

        String sub = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "dash" -> {
                if (plugin.projectionManager() != null) plugin.projectionManager().tryActivate(p);
                return true;
            }
            case "breaker" -> {
                if (plugin.projectionManager() != null) plugin.projectionManager().tryBreaker(p);
                return true;
            }
            case "cancel" -> {
                if (plugin.projectionManager() != null) plugin.projectionManager().tryCancel(p);
                return true;
            }
            case "unlock" -> {
                if (plugin.projectionManager() != null) plugin.projectionManager().tryUnlock(p);
                return true;
            }
            case "status" -> {
                if (plugin.projectionManager() == null) {
                    p.sendMessage(plugin.cfg().prefix() + "§cProjection system not loaded.");
                    return true;
                }
                ProjectionPlayerData data = plugin.projectionManager().getData(p.getUniqueId());
                if (data == null) {
                    p.sendMessage(plugin.cfg().prefix() + "§5Projection: §7IDLE");
                } else {
                    String lockName = "none";
                    if (data.lockedTarget != null) {
                        var e = org.bukkit.Bukkit.getEntity(data.lockedTarget);
                        if (e instanceof Player lp) lockName = lp.getName();
                        else if (e != null) lockName = e.getType().name();
                    }
                    p.sendMessage(plugin.cfg().prefix() + "§5Projection: §f" + data.state.name()
                            + " §8| §5Stack: §f" + data.stacks
                            + " §8| §5Step: §f" + data.stepIndex + "/" + 20
                            + " §8| §5Lock: §f" + lockName);
                }
                return true;
            }
            default -> {
                p.sendMessage(plugin.cfg().prefix() + "§5Projection Sorcery:");
                p.sendMessage("§f/" + label + " dash §7— Activate frame-stepping dash");
                p.sendMessage("§f/" + label + " breaker §7— Lunge back then shatter frozen target");
                p.sendMessage("§f/" + label + " cancel §7— Emergency exit (resets stacks)");
                p.sendMessage("§f/" + label + " unlock §7— Release lock-on");
                p.sendMessage("§f/" + label + " status §7— Show current state");
                p.sendMessage("§7Lock-on is automatic when you hit a target.");
                return true;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!(sender instanceof Player p)) return List.of();
        String assignedId = plugin.techniqueManager().getAssignedId(p.getUniqueId());
        if (!"projection".equalsIgnoreCase(assignedId)) return List.of();
        if (args.length == 1) return List.of("dash", "breaker", "cancel", "unlock", "status");
        return List.of();
    }
}