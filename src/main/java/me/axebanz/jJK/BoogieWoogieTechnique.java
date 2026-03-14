package me.axebanz.jJK;

import org.bukkit.entity.Player;

public final class BoogieWoogieTechnique implements Technique {

    @Override public String id() { return "boogie_woogie"; }
    @Override public String displayName() { return "§bBoogie Woogie"; }
    @Override public String hexColor() { return "#00BFFF"; }
    @Override public String glyphTag() { return "<glyph:technique_boogie_woogie:colorable>"; }
    @Override public String iconColor() { return "§b"; }

    @Override
    public void castAbility(Player player, AbilitySlot slot) {
        player.sendMessage(JJKCursedToolsPlugin.get().cfg().prefix() + "§cUse /boogiewoogie <clap|swap|clear>.");
    }
}