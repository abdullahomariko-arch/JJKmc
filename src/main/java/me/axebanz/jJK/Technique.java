package me.axebanz.jJK;

import org.bukkit.entity.Player;

public interface Technique {

    String id();
    String displayName();
    String hexColor(); // used by UI

    /**
     * Returns the Nexo glyph tag for this technique.
     * Format: <glyph:glyph_id>
     */
    default String glyphTag() {
        return "";
    }

    /**
     * Returns the color code for the icon.
     */
    default String iconColor() {
        return "§7";
    }

    void castAbility(Player player, AbilitySlot slot);

    default boolean canUse(Player p) {
        return true;
    }
}