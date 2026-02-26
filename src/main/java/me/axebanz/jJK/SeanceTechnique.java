package me.axebanz.jJK;

import org.bukkit.entity.Player;

public class SeanceTechnique implements Technique {
    private final JJKCursedToolsPlugin plugin;
    private final SeanceManager manager;

    public SeanceTechnique(JJKCursedToolsPlugin plugin, SeanceManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override public String getId() { return "seance"; }
    @Override public String getDisplayName() { return "§5Séance"; }

    @Override
    public void castAbility(Player player, String ability) {
        String techId = plugin.techniqueManager().getAssignedId(player.getUniqueId());
        if (!"seance".equalsIgnoreCase(techId)) {
            player.sendMessage(plugin.cfg().prefix() + "§cYou don't have the §5Séance§c technique.");
            return;
        }
        player.sendMessage(plugin.cfg().prefix() + "§5Ability: " + ability);
    }

    @Override public void onEquip(Player player) { player.sendMessage(plugin.cfg().prefix() + "§5Séance §7equipped!"); }
    @Override public void onUnequip(Player player) { player.sendMessage(plugin.cfg().prefix() + "§5Séance §7unequipped."); }
}
