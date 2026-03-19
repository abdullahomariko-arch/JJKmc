package me.axebanz.jJK;

import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * /sixtrait give <player> | /sixtrait remove <player>
 * Manages the Six Eyes trait.
 */
public final class CmdSixTrait implements CommandExecutor, TabCompleter {

    private final JJKCursedToolsPlugin plugin;

    public CmdSixTrait(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        String pref = plugin.cfg().prefix();

        if (!sender.hasPermission("jjk.sixtrait")) {
            sender.sendMessage(pref + "§cNo permission.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(pref + "§eUsage: /" + label + " <give|remove> <player>");
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(pref + "§cPlayer not found: §f" + args[1]);
            return true;
        }

        SixEyesTrait trait = plugin.sixEyes();
        if (trait == null) {
            sender.sendMessage(pref + "§cSix Eyes system not loaded.");
            return true;
        }

        switch (sub) {
            case "give" -> {
                trait.give(target);
                sender.sendMessage(pref + "§aGranted §bSix Eyes §ato §f" + target.getName() + "§a.");
                target.sendMessage(pref + "§bYou have been granted the §6Six Eyes§b trait!");
                target.sendMessage(pref + "§7Max CE level: §f200 §7| Limitless costs: §f~0");
                // Re-initialize CE to new max if needed
                plugin.ce().ensureInitialized(target.getUniqueId());
            }
            case "remove" -> {
                trait.remove(target);
                sender.sendMessage(pref + "§aRemoved §bSix Eyes §afrom §f" + target.getName() + "§a.");
                target.sendMessage(pref + "§7Six Eyes trait removed.");
            }
            case "check" -> {
                boolean has = trait.hasSixEyes(target);
                sender.sendMessage(pref + "§f" + target.getName() + " §7Six Eyes: "
                        + (has ? "§aActive" : "§cInactive"));
            }
            default -> sender.sendMessage(pref + "§cUsage: /" + label + " <give|remove|check> <player>");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!sender.hasPermission("jjk.sixtrait")) return List.of();
        if (args.length == 1) return List.of("give", "remove", "check");
        if (args.length == 2) {
            List<String> names = new ArrayList<>();
            Bukkit.getOnlinePlayers().forEach(p -> names.add(p.getName()));
            return names;
        }
        return List.of();
    }
}
