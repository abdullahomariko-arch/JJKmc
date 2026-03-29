package me.axebanz.jJK;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Soft-dependency bridge for Skript.
 * All methods silently do nothing if Skript is not installed.
 */
public final class SkriptBridge {

    private SkriptBridge() {}

    /** Returns true if Skript is loaded and enabled on this server. */
    public static boolean isSkriptEnabled() {
        Plugin skript = Bukkit.getPluginManager().getPlugin("Skript");
        return skript != null && skript.isEnabled();
    }

    /**
     * Dispatches a Skript-compatible command to trigger a visual script.
     * If Skript is not installed, this call is silently ignored.
     *
     * @param scriptName the name/key of the script to trigger (used as command argument)
     * @param player     the player involved in the visual
     * @param target     the target location for the visual effect
     */
    public static void triggerVisual(String scriptName, Player player, Location target) {
        if (!isSkriptEnabled()) return;
        // Dispatch a console command that a Skript script can listen to.
        // Scripts should listen for: on script command "jjk_visual <scriptName>"
        String cmd = "jjk_visual " + scriptName
                + " " + player.getName()
                + " " + target.getWorld().getName()
                + " " + target.getX()
                + " " + target.getY()
                + " " + target.getZ();
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
    }
}
