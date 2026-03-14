package me.axebanz.jJK;

import org.bukkit.entity.Player;

public final class CreationTechnique implements Technique {

    private final JJKCursedToolsPlugin plugin;

    public CreationTechnique(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public String id() { return "creation"; }
    @Override public String displayName() { return "§fCreation"; }
    @Override public String hexColor() { return "#FFFFFF"; }
    @Override public String glyphTag() { return "<glyph:technique_creation:colorable>"; }
    @Override public String iconColor() { return "§f"; }

    @Override
    public void castAbility(Player player, AbilitySlot slot) {
        player.sendMessage(plugin.cfg().prefix() + "§cUse /creation commands instead!");
    }

    @Override
    public boolean canUse(Player p) { return true; }
}