package me.axebanz.jJK;

import org.bukkit.entity.Player;

/**
 * Idle Death Gamble technique.
 * Bug Fix #3: castAbility checks that the player has "idle_death_gamble" assigned.
 */
public class IdleDeathGambleTechnique implements Technique {
    private final JJKCursedToolsPlugin plugin;
    private final IdleDeathGambleManager manager;

    public IdleDeathGambleTechnique(JJKCursedToolsPlugin plugin, IdleDeathGambleManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public String getId() { return "idle_death_gamble"; }

    @Override
    public String getDisplayName() { return "§eIdle Death Gamble"; }

    @Override
    public void castAbility(Player player, String ability) {
        // Bug Fix #3: Check player has idle_death_gamble technique
        String techId = plugin.techniqueManager().getAssignedId(player.getUniqueId());
        if (!"idle_death_gamble".equalsIgnoreCase(techId)) {
            player.sendMessage(plugin.cfg().prefix() + "§cYou don't have the §eIdle Death Gamble§c technique.");
            return;
        }
        switch (ability.toLowerCase()) {
            case "gamble" -> manager.triggerGamble(player);
            case "domain" -> manager.openDomain(player);
            default -> player.sendMessage(plugin.cfg().prefix() + "§cUnknown ability: " + ability);
        }
    }

    @Override
    public void onEquip(Player player) {
        player.sendMessage(plugin.cfg().prefix() + "§eIdle Death Gamble §7equipped!");
    }

    @Override
    public void onUnequip(Player player) {
        player.sendMessage(plugin.cfg().prefix() + "§eIdle Death Gamble §7unequipped.");
    }
}
