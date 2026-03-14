package me.axebanz.jJK;

import org.bukkit.command.CommandSender;

import java.util.List;

public final class CmdHelp implements SubCommand {

    private final JJKCursedToolsPlugin plugin;
    public CmdHelp(JJKCursedToolsPlugin plugin) { this.plugin = plugin; }

    @Override public String name() { return "help"; }
    @Override public String permission() { return ""; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        String p = plugin.cfg().prefix();
        sender.sendMessage(p + "§f/jjk give <player> <tool> [amount]");
        sender.sendMessage(p + "§f/jjk technique <1|2|3>");
        sender.sendMessage(p + "§f/jjk technique set <player> <technique>");
        sender.sendMessage(p + "§f/jjk status <player>");
        sender.sendMessage(p + "§f/jjk reload");
        return true;
    }

    @Override
    public List<String> tab(CommandSender sender, String[] args) {
        return List.of();
    }
}