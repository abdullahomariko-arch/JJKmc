package me.axebanz.jJK;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public final class CmdSetWaitingRoom implements SubCommand {

    private final JJKCursedToolsPlugin plugin;
    private final SeanceManager seanceManager;

    public CmdSetWaitingRoom(JJKCursedToolsPlugin plugin, SeanceManager seanceManager) {
        this.plugin = plugin;
        this.seanceManager = seanceManager;
    }

    @Override public String name() { return "setwaitingroom"; }
    @Override public String permission() { return "jjk.admin"; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        String pref = plugin.cfg().prefix();
        if (!(sender instanceof Player p)) {
            sender.sendMessage(pref + "§cPlayers only.");
            return true;
        }
        seanceManager.saveWaitingRoom(p.getLocation());
        sender.sendMessage(pref + "§aWaiting room location set to your current position.");
        return true;
    }

    @Override
    public List<String> tab(CommandSender sender, String[] args) {
        return List.of();
    }
}
