package me.axebanz.jJK;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;

/**
 * Placeholder for commands.java - acts as command registry helper.
 */
public class commands {
    // Static utility class for command registration helpers
    private commands() {}

    public static void registerIfPresent(JJKCursedToolsPlugin plugin, String name,
                                         CommandExecutor executor, TabCompleter completer) {
        if (plugin.getCommand(name) != null) {
            plugin.getCommand(name).setExecutor(executor);
            if (completer != null) {
                plugin.getCommand(name).setTabCompleter(completer);
            }
        } else {
            plugin.getLogger().warning("Command /" + name + " is missing from plugin.yml");
        }
    }
}
