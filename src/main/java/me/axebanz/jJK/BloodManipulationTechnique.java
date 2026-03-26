package me.axebanz.jJK;

import org.bukkit.entity.Player;

public final class BloodManipulationTechnique implements Technique {
    private final JJKCursedToolsPlugin plugin;

    public BloodManipulationTechnique(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public String id() { return "blood_manipulation"; }
    @Override public String displayName() { return "§4Blood Manipulation"; }
    @Override public String hexColor() { return "#8B0000"; }

    @Override
    public void castAbility(Player player, AbilitySlot slot) {
        BloodManipulationManager mgr = plugin.bloodManip();
        if (mgr == null) {
            player.sendMessage(plugin.cfg().prefix() + "§4Blood Manipulation §7— coming soon.");
            return;
        }
        switch (slot) {
            case ONE -> mgr.activateSupernova(player);
            case TWO -> mgr.castHarpoon(player);
        }
    }
}
