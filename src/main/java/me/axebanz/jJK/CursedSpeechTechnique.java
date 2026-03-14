package me.axebanz.jJK;

import org.bukkit.entity.Player;

public final class CursedSpeechTechnique implements Technique {

    @Override public String id() { return "cursed_speech"; }
    @Override public String displayName() { return "§fCursed Speech"; }
    @Override public String hexColor() { return "#7CFC00"; }
    @Override public String glyphTag() { return "<glyph:technique_cursed_speech:colorable>"; }
    @Override public String iconColor() { return "§a"; }

    @Override
    public void castAbility(Player player, AbilitySlot slot) {
        player.sendMessage(JJKCursedToolsPlugin.get().cfg().prefix() + "§cUse /cursedspeach <nomove|plummet|explode>.");
    }
}