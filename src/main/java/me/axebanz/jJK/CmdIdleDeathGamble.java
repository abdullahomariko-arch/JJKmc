package me.axebanz.jJK;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

/**
 * /idledeathgamble command handler.
 * Bug Fix #3: Checks that the player has "idle_death_gamble" technique assigned.
 */
public class CmdIdleDeathGamble implements CommandExecutor, TabCompleter {
    private final JJKCursedToolsPlugin plugin;
    private final IdleDeathGambleManager manager;

    public CmdIdleDeathGamble(JJKCursedToolsPlugin plugin, IdleDeathGambleManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(plugin.cfg().prefix() + "§cPlayers only.");
            return true;
        }

        // Bug Fix #3: technique exclusivity
        String techId = plugin.techniqueManager().getAssignedId(p.getUniqueId());
        if (!"idle_death_gamble".equalsIgnoreCase(techId)) {
            p.sendMessage(plugin.cfg().prefix() + "§cYou don't have the §eIdle Death Gamble§c technique.");
            return true;
        }

        if (args.length == 0) {
            p.sendMessage(plugin.cfg().prefix() + "§eIdle Death Gamble:");
            p.sendMessage("§f/" + label + " gamble §7— Trigger gamble");
            p.sendMessage("§f/" + label + " domain §7— Open domain");
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "gamble" -> manager.triggerGamble(p);
            case "domain" -> manager.openDomain(p);
            default -> p.sendMessage(plugin.cfg().prefix() + "§cUsage: /" + label + " <gamble|domain>");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!(sender instanceof Player)) return List.of();
        if (args.length == 1) return List.of("gamble", "domain");
        return List.of();
    }
}
