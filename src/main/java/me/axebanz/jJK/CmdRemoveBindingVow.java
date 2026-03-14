package me.axebanz.jJK;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public final class CmdRemoveBindingVow implements SubCommand {

    private final JJKCursedToolsPlugin plugin;

    public CmdRemoveBindingVow(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public String name() { return "removebindingvow"; }
    @Override public String permission() { return "jjk.admin"; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        String pref = plugin.cfg().prefix();

        // args[0] = "removebindingvow", args[1] = optional player name
        Player target;
        if (args.length >= 2) {
            target = org.bukkit.Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(pref + "§cPlayer not found: §f" + args[1]);
                return true;
            }
        } else if (sender instanceof Player p) {
            target = p;
        } else {
            sender.sendMessage(pref + "§cUsage: /jjk removebindingvow <player>");
            return true;
        }

        PlayerProfile prof = plugin.data().get(target.getUniqueId());
        prof.seanceBindingVowActive = false;
        prof.strawDollBindingVowActive = false;
        plugin.data().save(target.getUniqueId());

        target.sendMessage(pref + "§aBinding Vow removed. All abilities restored to normal.");
        if (!sender.equals(target)) {
            sender.sendMessage(pref + "§aRemoved binding vow from §f" + target.getName() + "§a.");
        }
        return true;
    }

    @Override
    public List<String> tab(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> names = new java.util.ArrayList<>();
            for (Player p : org.bukkit.Bukkit.getOnlinePlayers()) names.add(p.getName());
            return names;
        }
        return List.of();
    }
}
