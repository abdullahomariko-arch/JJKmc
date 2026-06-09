package me.axebanz.jJK;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;

public final class GraniteChargeBar {

    private static final Key FONT_BAR = Key.key("mybeam", "bar_single");
    private static final String STAGES = "0123456789ABCDEF";

    private GraniteChargeBar() {}

    public static int getStage(int chargePercent) {
        int pct = Math.max(0, Math.min(100, chargePercent));
        int stage = (int) Math.floor((pct / 100.0) * 15.0);
        return Math.max(0, Math.min(15, stage));
    }

    public static char getStageChar(int chargePercent) {
        return STAGES.charAt(getStage(chargePercent));
    }

    public static Component getBarComponent(int chargePercent) {
        return Component.text(String.valueOf(getStageChar(chargePercent))).font(FONT_BAR);
    }
}