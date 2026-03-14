package me.axebanz.jJK;

import org.bukkit.entity.Player;

public final class StrawDollTechnique implements Technique {

    private final JJKCursedToolsPlugin plugin;

    public StrawDollTechnique(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public String id() { return "strawdoll"; }
    @Override public String displayName() { return "Straw Doll Technique"; }
    @Override public String hexColor() { return "#D4A017"; }
    @Override public String glyphTag() { return "<glyph:technique_strawdoll:colorable>"; }
    @Override public String iconColor() { return "§6"; }

    @Override
    public void castAbility(Player player, AbilitySlot slot) {
        StrawDollManager mgr = plugin.strawDollManager();
        if (mgr == null) return;
        switch (slot) {
            case ONE -> mgr.activateResonance(player);
            case TWO -> mgr.activateHairpin(player);
            default -> { }
        }
    }
}