package me.axebanz.jJK;

import org.bukkit.entity.Player;

public final class TenShadowsTechnique implements Technique {

    private final JJKCursedToolsPlugin plugin;

    public TenShadowsTechnique(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public String id() { return "ten_shadows"; }
    @Override public String displayName() { return "§8Ten Shadows Technique"; }
    @Override public String hexColor() { return "#1A1A2E"; }
    @Override public String glyphTag() { return "<glyph:technique_ten_shadows:colorable>"; }
    @Override public String iconColor() { return "§8"; }

    @Override
    public boolean canUse(Player p) { return true; }

    @Override
    public void castAbility(Player player, AbilitySlot slot) {
        switch (slot) {
            case ONE -> {
                // Summon selected from scroll wheel
                TenShadowsProfile prof = plugin.tenShadows().getProfile(player.getUniqueId());
                if (prof.activeSummonId != null) {
                    plugin.tenShadows().dismiss(player);
                } else {
                    TenShadowsSelectionUI ui = new TenShadowsSelectionUI(plugin);
                    ShikigamiType selected = ui.getSelected(prof);
                    if (selected != null) {
                        plugin.tenShadows().trySummon(player, selected);
                    } else {
                        player.sendMessage(plugin.cfg().prefix() + "§cNo shikigami available. Sneak + scroll to select.");
                    }
                }
            }
            case TWO -> {
                // Show selection wheel
                TenShadowsProfile prof = plugin.tenShadows().getProfile(player.getUniqueId());
                TenShadowsSelectionUI ui = new TenShadowsSelectionUI(plugin);
                ui.showSelectionWheel(player, prof);
            }
            case THREE -> {
                // Show status
                TenShadowsProfile prof = plugin.tenShadows().getProfile(player.getUniqueId());
                TenShadowsSelectionUI ui = new TenShadowsSelectionUI(plugin);
                ui.showStatusList(player, prof);
            }
        }
    }
}