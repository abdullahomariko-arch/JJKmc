package me.axebanz.jJK;

import org.bukkit.entity.Player;

public class GravityTechnique implements Technique {
    private final JJKCursedToolsPlugin plugin;

    public GravityTechnique(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public String getId() { return "gravity"; }
    @Override public String getDisplayName() { return "§8Gravity"; }

    @Override
    public void castAbility(Player player, String ability) {
        String techId = plugin.techniqueManager().getAssignedId(player.getUniqueId());
        if (!"gravity".equalsIgnoreCase(techId)) {
            player.sendMessage(plugin.cfg().prefix() + "§cYou don't have the §8Gravity§c technique.");
            return;
        }
        player.sendMessage(plugin.cfg().prefix() + "§8Gravity ability: " + ability);
    }

    @Override public void onEquip(Player player) { player.sendMessage(plugin.cfg().prefix() + "§8Gravity §7equipped!"); }
    @Override public void onUnequip(Player player) { player.sendMessage(plugin.cfg().prefix() + "§8Gravity §7unequipped."); }
}
