package me.axebanz.jJK;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public class CmdHelp implements CommandExecutor, TabCompleter {
    private final JJKCursedToolsPlugin plugin;

    public CmdHelp(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        sender.sendMessage(plugin.cfg().prefix() + "§6JJK Plugin Help:");
        sender.sendMessage("§f/technique §7— Manage techniques");
        sender.sendMessage("§f/projection §7— Projection Sorcery abilities");
        sender.sendMessage("§f/creation §7— Creation abilities");
        sender.sendMessage("§f/idledeathgamble §7— Idle Death Gamble abilities");
        sender.sendMessage("§f/seance §7— Perform séance");
        sender.sendMessage("§f/strawdoll §7— Straw Doll abilities");
        sender.sendMessage("§f/domain §7— Domain Expansion");
        sender.sendMessage("§f/status §7— View your status");
        if (sender.hasPermission("jjk.admin")) {
            sender.sendMessage("§f/jjkgive §7— Give JJK items (admin)");
            sender.sendMessage("§f/permadeath §7— Manage permadeath (admin)");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        return List.of();
    }
}
