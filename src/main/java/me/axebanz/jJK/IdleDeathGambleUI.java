package me.axebanz.jJK;

import org.bukkit.entity.Player;

public final class IdleDeathGambleUI {

    private IdleDeathGambleUI() {}

    public static void showGameUI(Player p, IdleDeathGambleManager.IDGGameState state) {
        int spinsLeft = IdleDeathGambleManager.MAX_SPINS - state.spinCount;
        String indicators = "§a" + state.greenIndicators + "G §c" + state.redIndicators + "R §6" + state.goldIndicators + "Au";
        if (state.rainbowIndicator) indicators += " §d★";
        String bonus = state.riichiBonus > 0 ? " §8+§6" + state.riichiBonus + "%" : "";
        p.sendActionBar("§d★ IDG §8| §fSpins: §e" + spinsLeft + "§f/" + IdleDeathGambleManager.MAX_SPINS
                + " §8| §7Indicators: " + indicators + bonus + " §8| §d1/239");
    }

    public static void showJackpotUI(Player p, IdleDeathGambleManager.IDGGameState state, long ticksRemaining) {
        long seconds = ticksRemaining / 20;
        long minutes = seconds / 60;
        long secs = seconds % 60;
        String timeStr = String.format("%d:%02d", minutes, secs);
        p.sendActionBar("§6§l★ JACKPOT ★ §8| §fTime: §e" + timeStr + " §8| §6∞ CE §8| §c♥ AUTO-HEAL");
    }
}
