package me.axebanz.jJK;

import org.bukkit.entity.Player;

public final class EnergyDischargeTechnique implements Technique {

    private final JJKCursedToolsPlugin plugin;

    public EnergyDischargeTechnique(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public String id() { return "energy_discharge"; }
    @Override public String displayName() { return "§b⚡ Energy Discharge"; }
    @Override public String hexColor() { return "#00BFFF"; }

    @Override
    public void castAbility(Player player, AbilitySlot slot) {
        EnergyDischargeManager mgr = plugin.energyDischarge();
        if (mgr == null) {
            player.sendMessage(plugin.cfg().prefix() + "§b⚡ Energy Discharge §7— coming soon.");
            return;
        }
        switch (slot) {
            case ONE -> mgr.startTrackingCharge(player);
            case TWO -> mgr.startBlastCharge(player);
        }
    }
}
