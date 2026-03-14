package me.axebanz.jJK;

import org.bukkit.entity.Player;

public final class ProjectionTechnique implements Technique {

    private final JJKCursedToolsPlugin plugin;

    public ProjectionTechnique(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public String id() { return "projection"; }
    @Override public String displayName() { return "§5Projection Sorcery"; }
    @Override public String hexColor() { return "#8C82FF"; }
    @Override public String glyphTag() { return "<glyph:technique_projection:colorable>"; }
    @Override public String iconColor() { return "§9"; }

    @Override
    public boolean canUse(Player p) { return true; }

    @Override
    public void castAbility(Player player, AbilitySlot slot) {
        if (plugin.projectionManager() == null) return;
        switch (slot) {
            case ONE -> plugin.projectionManager().tryActivate(player);
            case TWO -> plugin.projectionManager().tryBreaker(player);
            case THREE -> plugin.projectionManager().tryCancel(player);
            default -> player.sendMessage(plugin.cfg().prefix() + "§5Projection: §7Slot 1 = Dash, Slot 2 = Breaker, Slot 3 = Cancel");
        }
    }
}