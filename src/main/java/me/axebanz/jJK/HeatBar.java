package me.axebanz.jJK;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;

public final class HeatBar {

    private static final Key FONT_BAR = Key.key("mybeam", "heat_bar");
    private static final String STAGES = "0123456789ABCDEF";

    private HeatBar() {}

    public static int getStage(int heatPercent) {
        int pct = Math.max(0, Math.min(100, heatPercent));
        int stage = (int) Math.floor((pct / 100.0) * 15.0);
        return Math.max(0, Math.min(15, stage));
    }

    public static char getStageChar(int heatPercent) {
        return STAGES.charAt(getStage(heatPercent));
    }

    public static Component getBarComponent(int heatPercent) {
        return Component.text(String.valueOf(getStageChar(heatPercent))).font(FONT_BAR);
    }
}