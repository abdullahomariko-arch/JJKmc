package me.axebanz.jJK;

import org.bukkit.entity.Player;

public class CreationTechnique implements Technique {
    private final JJKCursedToolsPlugin plugin;
    private final CreationManager manager;

    public CreationTechnique(JJKCursedToolsPlugin plugin, CreationManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override public String getId() { return "creation"; }
    @Override public String getDisplayName() { return "§dCreation"; }

    @Override
    public void castAbility(Player player, String ability) {
        String techId = plugin.techniqueManager().getAssignedId(player.getUniqueId());
        if (!"creation".equalsIgnoreCase(techId)) {
            player.sendMessage(plugin.cfg().prefix() + "§cYou don't have the §dCreation§c technique.");
            return;
        }
        switch (ability.toLowerCase()) {
            case "create" -> manager.openCreationMenu(player);
            case "remove" -> manager.removeConstructs(player);
            default -> player.sendMessage(plugin.cfg().prefix() + "§cUnknown ability: " + ability);
        }
    }

    @Override public void onEquip(Player player) { player.sendMessage(plugin.cfg().prefix() + "§dCreation §7equipped!"); }
    @Override public void onUnequip(Player player) { player.sendMessage(plugin.cfg().prefix() + "§dCreation §7unequipped."); }
}
