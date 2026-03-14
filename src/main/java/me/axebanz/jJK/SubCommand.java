package me.axebanz.jJK;

import org.bukkit.command.CommandSender;

import java.util.List;

public interface SubCommand {
    String name();
    String permission();
    boolean execute(CommandSender sender, String[] args);
    List<String> tab(CommandSender sender, String[] args);
}
