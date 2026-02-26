package me.axebanz.jJK;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class CombatListener implements Listener {
    private final JJKCursedToolsPlugin plugin;
    private final AbilityService abilityService;

    public CombatListener(JJKCursedToolsPlugin plugin, AbilityService abilityService) {
        this.plugin = plugin;
        this.abilityService = abilityService;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        // Check for split soul katana in hand
        if (attacker.getInventory().getItemInMainHand().getItemMeta() != null) {
            String displayName = attacker.getInventory().getItemInMainHand().getItemMeta().getDisplayName();
            if (displayName != null && displayName.contains("Split Soul Katana")) {
                abilityService.handleSplitSoulHit(attacker, event.getEntity(), event.getDamage());
            }
        }
    }
}
