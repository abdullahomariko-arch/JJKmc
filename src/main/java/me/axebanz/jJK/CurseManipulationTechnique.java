package me.axebanz.jJK;

import org.bukkit.entity.Player;

/**
 * Curse Manipulation Technique — placeholder for future implementation.
 */
public final class CurseManipulationTechnique implements Technique {

    private final JJKCursedToolsPlugin plugin;

    public CurseManipulationTechnique(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public String id() { return "curse_manipulation"; }
    @Override public String displayName() { return "§4Curse Manipulation"; }
    @Override public String hexColor() { return "#8B0000"; }
    @Override public String glyphTag() { return ""; }
    @Override public String iconColor() { return "§4"; }

    @Override
    public boolean canUse(Player p) { return true; }

    @Override
    public void castAbility(Player player, AbilitySlot slot) {
        player.sendMessage(plugin.cfg().prefix() + "§4Curse Manipulation §7— coming soon.");
    }
}
