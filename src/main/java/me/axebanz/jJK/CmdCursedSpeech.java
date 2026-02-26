package me.axebanz.jJK;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

public class CmdCursedSpeech implements CommandExecutor, TabCompleter {
    private final JJKCursedToolsPlugin plugin;
    private final CursedSpeechManager manager;

    public CmdCursedSpeech(JJKCursedToolsPlugin plugin, CursedSpeechManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(plugin.cfg().prefix() + "§cPlayers only.");
            return true;
        }
        String techId = plugin.techniqueManager().getAssignedId(p.getUniqueId());
        if (!"cursedspeech".equalsIgnoreCase(techId)) {
            p.sendMessage(plugin.cfg().prefix() + "§cYou don't have the §7Cursed Speech§c technique.");
            return true;
        }
        String command = args.length > 0 ? String.join(" ", args) : "silence";
        manager.activateCommand(p, command);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) return List.of("silence", "freeze", "burn");
        return List.of();
    }
}
