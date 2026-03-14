package me.axebanz.jJK;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

public final class WheelUI {

    private final JJKCursedToolsPlugin plugin;

    public WheelUI(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    public void showMode(Player p, AdaptationCategory mode, int stacks, int maxStacks) {
        String icon = "■";
        String color = modeColor(mode);

        p.sendActionBar(color + icon + " §fDIVINE WHEEL §8|§r " + color + mode.displayName() + " §7(" + stacks + "/" + maxStacks + ")");

        p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 1.2f);
    }

    public void showStackGain(Player p, AdaptationCategory mode, int stacks, int maxStacks) {
        String icon = "■";
        String color = modeColor(mode);
        p.sendActionBar(color + icon + " §fADAPTING §8|§r " + color + mode.displayName() + " §7(" + stacks + "/" + maxStacks + ")");
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.35f, 1.7f);
    }

    public String modeColor(AdaptationCategory mode) {
        return switch (mode) {
            case MELEE -> "§c";
            case PROJECTILE -> "§9";
            case EXPLOSION -> "§6";
            case FIRE -> "§e";
            case LIGHTNING -> "§b";
            case TECHNIQUE -> "§5";
            case TRUE_DAMAGE -> "§f";
        };
    }

    public void showNotWearing(Player p) {
        p.sendActionBar("§7■ §fDIVINE WHEEL §8|§r §cNot equipped");
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.6f);
    }
}