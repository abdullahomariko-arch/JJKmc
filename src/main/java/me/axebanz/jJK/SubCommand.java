package me.axebanz.jJK;

import org.bukkit.entity.Player;

public interface SubCommand {
    String getName();
    String getDescription();
    String getUsage();
    boolean execute(Player player, String[] args);
}
