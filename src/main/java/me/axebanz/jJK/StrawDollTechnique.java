package me.axebanz.jJK;

import org.bukkit.entity.Player;

public class StrawDollTechnique implements Technique {
    private final JJKCursedToolsPlugin plugin;
    private final StrawDollManager manager;

    public StrawDollTechnique(JJKCursedToolsPlugin plugin, StrawDollManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override public String getId() { return "strawdoll"; }
    @Override public String getDisplayName() { return "§6Straw Doll"; }

    @Override
    public void castAbility(Player player, String ability) {
        String techId = plugin.techniqueManager().getAssignedId(player.getUniqueId());
        if (!"strawdoll".equalsIgnoreCase(techId)) {
            player.sendMessage(plugin.cfg().prefix() + "§cYou don't have the §6Straw Doll§c technique.");
            return;
        }
        switch (ability.toLowerCase()) {
            case "resonance" -> manager.activateResonance(player);
            case "hairpin" -> manager.activateHairpin(player);
            default -> player.sendMessage(plugin.cfg().prefix() + "§cUnknown ability: " + ability);
        }
    }

    @Override
    public void onEquip(Player player) {
        player.sendMessage(plugin.cfg().prefix() + "§6Straw Doll §7equipped!");
    }

    @Override
    public void onUnequip(Player player) {
        player.sendMessage(plugin.cfg().prefix() + "§6Straw Doll §7unequipped.");
    }
}
