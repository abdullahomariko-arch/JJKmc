package me.axebanz.jJK;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public final class CmdStatus implements SubCommand {

    private final JJKCursedToolsPlugin plugin;
    public CmdStatus(JJKCursedToolsPlugin plugin) { this.plugin = plugin; }

    @Override public String name() { return "status"; }
    @Override public String permission() { return "jjk.status"; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        String pref = plugin.cfg().prefix();
        if (args.length < 2) {
            sender.sendMessage(pref + "§cUsage: /jjk status <player>");
            return true;
        }

        Player t = Bukkit.getPlayer(args[1]);
        if (t == null) {
            sender.sendMessage(pref + "§cPlayer not found.");
            return true;
        }

        var uuid = t.getUniqueId();
        int ce = plugin.ce().get(uuid);
        String tech = plugin.techniqueManager().techniqueName(uuid);
        boolean nullified = plugin.nullify().isNullified(uuid);
        long nullRem = plugin.nullify().remainingSeconds(uuid);
        boolean regenLocked = plugin.regenLock().isLocked(uuid);
        long regenRem = plugin.regenLock().remainingSeconds(uuid);

        sender.sendMessage(pref + "§fStatus: §e" + t.getName());
        sender.sendMessage("§7CE: §f" + ce + "§7/§f" + plugin.cfg().ceMax());
        sender.sendMessage("§7Technique: §f" + tech);
        sender.sendMessage("§7Nullified: " + (nullified ? "§cYES §7(" + TimeFmt.mmss(nullRem) + ")" : "§aNO"));
        sender.sendMessage("§7No-Regen: " + (regenLocked ? "§cYES §7(" + TimeFmt.mmss(regenRem) + ")" : "§aNO"));
        return true;
    }

    @Override
    public List<String> tab(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
        return List.of();
    }
}