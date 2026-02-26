package me.axebanz.jJK;

import org.bukkit.entity.Player;

/**
 * Projection Sorcery technique implementation.
 * Bug Fix #3: castAbility checks that the player has "projection" assigned.
 */
public class ProjectionTechnique implements Technique {
    private final JJKCursedToolsPlugin plugin;
    private final ProjectionManager manager;

    public ProjectionTechnique(JJKCursedToolsPlugin plugin, ProjectionManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public String getId() { return "projection"; }

    @Override
    public String getDisplayName() { return "§bProjection Sorcery"; }

    @Override
    public void castAbility(Player player, String ability) {
        // Bug Fix #3: Check player has projection technique
        String techId = plugin.techniqueManager().getAssignedId(player.getUniqueId());
        if (!"projection".equalsIgnoreCase(techId)) {
            player.sendMessage(plugin.cfg().prefix() + "§cYou don't have the §bProjection Sorcery§c technique.");
            return;
        }
        switch (ability.toLowerCase()) {
            case "dash" -> manager.commitProgramming(player, plugin.cfg().projectionDashDistance());
            case "program" -> manager.startProgramming(player);
            case "breaker_lunge" -> manager.activateBreakerLunge(player);
            case "breaker_back" -> manager.activateBreakerBack(player);
            default -> player.sendMessage(plugin.cfg().prefix() + "§cUnknown ability: " + ability);
        }
    }

    @Override
    public void onEquip(Player player) {
        player.sendMessage(plugin.cfg().prefix() + "§bProjection Sorcery §7equipped!");
    }

    @Override
    public void onUnequip(Player player) {
        manager.onPlayerQuit(player.getUniqueId());
        player.sendMessage(plugin.cfg().prefix() + "§bProjection Sorcery §7unequipped.");
    }
}
