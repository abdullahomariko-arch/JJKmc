package me.axebanz.jJK;

import org.bukkit.entity.Player;

public final class SeanceTechnique implements Technique {

    private final JJKCursedToolsPlugin plugin;

    public SeanceTechnique(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public String id() { return "seance"; }
    @Override public String displayName() { return "Séance"; }
    @Override public String hexColor() { return "#8B4A8B"; }
    @Override public String glyphTag() { return "<glyph:technique_seance:colorable>"; }
    @Override public String iconColor() { return "§5"; }

    @Override
    public void castAbility(Player player, AbilitySlot slot) {
        player.sendMessage(plugin.cfg().prefix() + "§5Use §d/seance activate§5 near an armor stand holding a cursed body to begin the séance.");
    }
}