package me.axebanz.jJK;

import org.bukkit.entity.Player;

/**
 * Limitless Technique — requires Six Eyes trait for full power.
 * Five selectable abilities: Infinity, Blue, Red, Hollow Purple, Infinite Void.
 * Maximum Output (Blue Max / Red Max) and Purple Nuke are internal mechanics only.
 */
public final class LimitlessTechnique implements Technique {

    private final JJKCursedToolsPlugin plugin;

    public LimitlessTechnique(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public String id() { return "limitless"; }
    @Override public String displayName() { return "§bLimitless"; }
    @Override public String hexColor() { return "#00BFFF"; }
    @Override public String glyphTag() { return "<glyph:technique_limitless:colorable>"; }
    @Override public String iconColor() { return "§b"; }

    @Override
    public boolean canUse(Player p) { return true; }

    @Override
    public void castAbility(Player player, AbilitySlot slot) {
        LimitlessManager mgr = plugin.limitless();
        if (mgr == null) {
            player.sendMessage(plugin.cfg().prefix() + "§bLimitless §7is not ready.");
            return;
        }
        switch (slot) {
            case ONE   -> mgr.toggleInfinity(player);
            case TWO   -> mgr.castBlue(player);
            case THREE -> mgr.castRed(player);
            case FOUR  -> mgr.castHollowPurple(player);
            case FIVE  -> mgr.castInfiniteVoid(player);
            default -> { }
        }
    }
}

