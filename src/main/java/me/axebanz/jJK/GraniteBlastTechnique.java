package me.axebanz.jJK;

import org.bukkit.entity.Player;

public final class GraniteBlastTechnique implements Technique {
    @Override public String id() { return "granite_blast"; }
    @Override public String displayName() { return "§6Granite Blast"; }
    @Override public String hexColor() { return "#CD853F"; }
    @Override
    public void castAbility(Player player, AbilitySlot slot) {
        player.sendMessage("§6Granite Blast §7— coming soon.");
    }
}
