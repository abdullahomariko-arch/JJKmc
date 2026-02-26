package me.axebanz.jJK;

import org.bukkit.entity.Player;

public class CursedSpeechTechnique implements Technique {
    private final JJKCursedToolsPlugin plugin;
    private final CursedSpeechManager manager;

    public CursedSpeechTechnique(JJKCursedToolsPlugin plugin, CursedSpeechManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override public String getId() { return "cursedspeech"; }
    @Override public String getDisplayName() { return "§7Cursed Speech"; }

    @Override
    public void castAbility(Player player, String ability) {
        String techId = plugin.techniqueManager().getAssignedId(player.getUniqueId());
        if (!"cursedspeech".equalsIgnoreCase(techId)) {
            player.sendMessage(plugin.cfg().prefix() + "§cYou don't have the §7Cursed Speech§c technique.");
            return;
        }
        manager.activateCommand(player, ability);
    }

    @Override public void onEquip(Player player) { player.sendMessage(plugin.cfg().prefix() + "§7Cursed Speech §7equipped!"); }
    @Override public void onUnequip(Player player) { player.sendMessage(plugin.cfg().prefix() + "§7Cursed Speech §7unequipped."); }
}
