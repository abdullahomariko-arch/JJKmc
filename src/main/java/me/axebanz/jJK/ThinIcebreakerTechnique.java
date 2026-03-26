package me.axebanz.jJK;

import org.bukkit.entity.Player;

public final class ThinIcebreakerTechnique implements Technique {
    @Override public String id() { return "thin_icebreaker"; }
    @Override public String displayName() { return "§bThin Icebreaker"; }
    @Override public String hexColor() { return "#87CEEB"; }
    @Override
    public void castAbility(Player player, AbilitySlot slot) {
        player.sendMessage("§bThin Icebreaker §7— coming soon.");
    }
}
