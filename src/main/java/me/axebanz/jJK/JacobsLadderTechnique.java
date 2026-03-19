package me.axebanz.jJK;

import org.bukkit.entity.Player;

/**
 * Jacob's Ladder Technique — placeholder for future implementation.
 */
public final class JacobsLadderTechnique implements Technique {

    private final JJKCursedToolsPlugin plugin;

    public JacobsLadderTechnique(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public String id() { return "jacobs_ladder"; }
    @Override public String displayName() { return "§eJacob's Ladder"; }
    @Override public String hexColor() { return "#FFFF00"; }
    @Override public String glyphTag() { return ""; }
    @Override public String iconColor() { return "§e"; }

    @Override
    public boolean canUse(Player p) { return true; }

    @Override
    public void castAbility(Player player, AbilitySlot slot) {
        player.sendMessage(plugin.cfg().prefix() + "§eJacob's Ladder §7— coming soon.");
    }
}
