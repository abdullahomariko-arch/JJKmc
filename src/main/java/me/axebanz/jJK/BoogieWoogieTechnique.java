package me.axebanz.jJK;

import org.bukkit.entity.Player;

public class BoogieWoogieTechnique implements Technique {
    private final JJKCursedToolsPlugin plugin;
    private final BoogieWoogieManager manager;

    public BoogieWoogieTechnique(JJKCursedToolsPlugin plugin, BoogieWoogieManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override public String getId() { return "boogiewoogie"; }
    @Override public String getDisplayName() { return "§eBoogie Woogie"; }

    @Override
    public void castAbility(Player player, String ability) {
        String techId = plugin.techniqueManager().getAssignedId(player.getUniqueId());
        if (!"boogiewoogie".equalsIgnoreCase(techId)) {
            player.sendMessage(plugin.cfg().prefix() + "§cYou don't have the §eBoogie Woogie§c technique.");
            return;
        }
        switch (ability.toLowerCase()) {
            case "swap" -> manager.activateSwap(player);
            default -> player.sendMessage(plugin.cfg().prefix() + "§cUnknown ability: " + ability);
        }
    }

    @Override public void onEquip(Player player) { player.sendMessage(plugin.cfg().prefix() + "§eBoogie Woogie §7equipped!"); }
    @Override public void onUnequip(Player player) { player.sendMessage(plugin.cfg().prefix() + "§eBoogie Woogie §7unequipped."); }
}
