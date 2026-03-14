package me.axebanz.jJK;

import org.bukkit.command.CommandSender;

import java.util.List;

public final class CmdReload implements SubCommand {

    private final JJKCursedToolsPlugin plugin;
    public CmdReload(JJKCursedToolsPlugin plugin) { this.plugin = plugin; }

    @Override public String name() { return "reload"; }
    @Override public String permission() { return "jjk.reload"; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        plugin.reloadAll();
        sender.sendMessage(plugin.cfg().prefix() + "§aReloaded config.");
        return true;
    }

    @Override
    public List<String> tab(CommandSender sender, String[] args) {
        return List.of();
    }
}
