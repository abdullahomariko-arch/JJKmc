package me.axebanz.jJK;

import org.bukkit.entity.Player;

public final class IceFormationTechnique implements Technique {
    private final JJKCursedToolsPlugin plugin;
    public IceFormationTechnique(JJKCursedToolsPlugin plugin) { this.plugin = plugin; }
    @Override public String id() { return "ice_formation"; }
    @Override public String displayName() { return "§bIce Formation"; }
    @Override public String hexColor() { return "#ADD8E6"; }
    @Override
    public void castAbility(Player player, AbilitySlot slot) {
        IceFormationManager mgr = plugin.iceFormation();
        if (mgr == null) {
            player.sendMessage(plugin.cfg().prefix() + "§bIce Formation §7— coming soon.");
            return;
        }
        switch (slot) {
            case ONE -> mgr.castFrostCalm(player);
            case TWO -> mgr.castIcefall(player);
        }
    }
}
