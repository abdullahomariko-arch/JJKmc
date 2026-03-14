package me.axebanz.jJK;

import org.bukkit.entity.Player;

public final class CopyTechnique implements Technique {

    private final JJKCursedToolsPlugin plugin;

    public CopyTechnique(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public String id() { return "copy"; }
    @Override public String displayName() { return "§dCopy"; }
    @Override public String hexColor() { return "#FF66CC"; }
    @Override public String glyphTag() { return "<glyph:technique_copy:colorable>"; }
    @Override public String iconColor() { return "§d"; }

    @Override
    public boolean canUse(Player p) {
        return plugin.copy().canUseCopy(p);
    }

    @Override
    public void castAbility(Player player, AbilitySlot slot) {
        player.sendMessage(plugin.cfg().prefix() + "§cUse /copy <summon|dismiss|beam|storage|ct|shuffle|return>.");
    }
}