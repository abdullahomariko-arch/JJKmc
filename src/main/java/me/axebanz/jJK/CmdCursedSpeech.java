package me.axebanz.jJK;

import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

public final class CmdCursedSpeech implements CommandExecutor, TabCompleter {

    private final JJKCursedToolsPlugin plugin;
    private final CursedSpeechManager cursedSpeech;

    public CmdCursedSpeech(JJKCursedToolsPlugin plugin, CursedSpeechManager cursedSpeech) {
        this.plugin = plugin;
        this.cursedSpeech = cursedSpeech;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(plugin.cfg().prefix() + "§cPlayers only.");
            return true;
        }

        // FIXED: Technique exclusivity — must have cursed_speech equipped
        String assignedId = plugin.techniqueManager().getAssignedId(p.getUniqueId());
        if (!"cursed_speech".equalsIgnoreCase(assignedId)) {
            p.sendMessage(plugin.cfg().prefix() + "§cYou don't have §fCursed Speech§c equipped.");
            return true;
        }

        if (args.length == 0) {
            p.sendMessage(plugin.cfg().prefix() + "§cUsage: /" + label + " <nomove|plummet|explode>");
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "nomove", "dontmove", "dont_move", "no_move" -> cursedSpeech.castNoMove(p);
            case "plummet" -> cursedSpeech.castPlummet(p);
            case "explode" -> cursedSpeech.castExplode(p);
            default -> p.sendMessage(plugin.cfg().prefix() + "§cUsage: /" + label + " <nomove|plummet|explode>");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player p)) return List.of();
        String assignedId = plugin.techniqueManager().getAssignedId(p.getUniqueId());
        if (!"cursed_speech".equalsIgnoreCase(assignedId)) return List.of();

        if (args.length == 1) return List.of("nomove", "plummet", "explode");
        return List.of();
    }
}