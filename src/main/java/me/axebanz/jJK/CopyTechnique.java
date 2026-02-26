package me.axebanz.jJK;

import org.bukkit.entity.Player;

public class CopyTechnique implements Technique {
    private final JJKCursedToolsPlugin plugin;
    private final CopyManager manager;

    public CopyTechnique(JJKCursedToolsPlugin plugin, CopyManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override public String getId() { return "copy"; }
    @Override public String getDisplayName() { return "§aCopy"; }

    @Override
    public void castAbility(Player player, String ability) {
        String techId = plugin.techniqueManager().getAssignedId(player.getUniqueId());
        if (!"copy".equalsIgnoreCase(techId)) {
            player.sendMessage(plugin.cfg().prefix() + "§cYou don't have the §aCopy§c technique.");
            return;
        }
        player.sendMessage(plugin.cfg().prefix() + "§aAbility: " + ability);
    }

    @Override public void onEquip(Player player) { player.sendMessage(plugin.cfg().prefix() + "§aCopy §7equipped!"); }
    @Override public void onUnequip(Player player) { player.sendMessage(plugin.cfg().prefix() + "§aCopy §7unequipped."); }
}
