package me.axebanz.jJK;

import org.bukkit.entity.Player;

/**
 * Limitless Technique — requires Six Eyes trait for full power.
 * Abilities: Infinity, Blue, Blue Max, Red, Red Max, Hollow Purple, Hollow Purple Nuke,
 * Domain Expansion: Infinite Void.
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
            player.sendMessage(plugin.cfg().prefix() + "§bLimitless §7— coming soon.");
            return;
        }
        switch (slot) {
            case ONE -> mgr.toggleInfinity(player);
            case TWO -> mgr.castBlue(player);
            default -> { }
        }
    }
}

