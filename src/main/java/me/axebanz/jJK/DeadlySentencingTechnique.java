package me.axebanz.jJK;

import org.bukkit.entity.Player;

/**
 * Deadly Sentencing Technique — uses Judge's Hammer weapon.
 * Ability 1: Hammer Barrage — rapid 5 hits
 * Ability 2: Big Hammer Slam — charged AOE slam
 * Domain: Deadly Sentencing — Judgeman presides over a trial
 */
public final class DeadlySentencingTechnique implements Technique {

    private final JJKCursedToolsPlugin plugin;

    public DeadlySentencingTechnique(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public String id() { return "deadly_sentencing"; }
    @Override public String displayName() { return "§6Deadly Sentencing"; }
    @Override public String hexColor() { return "#FFD700"; }
    @Override public String glyphTag() { return ""; }
    @Override public String iconColor() { return "§6"; }

    @Override
    public boolean canUse(Player p) { return true; }

    @Override
    public void castAbility(Player player, AbilitySlot slot) {
        DeadlySentencingManager mgr = plugin.deadlySentencing();
        if (mgr == null) return;
        switch (slot) {
            case ONE -> mgr.activateHammerBarrage(player);
            case TWO -> mgr.startHammerSlamCharge(player);
            default -> { }
        }
    }
}
