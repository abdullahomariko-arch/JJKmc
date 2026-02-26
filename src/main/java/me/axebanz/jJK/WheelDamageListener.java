package me.axebanz.jJK;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class WheelDamageListener implements Listener {
    private final JJKCursedToolsPlugin plugin;
    private final WheelTierManager tierManager;

    public WheelDamageListener(JJKCursedToolsPlugin plugin, WheelTierManager tierManager) {
        this.plugin = plugin;
        this.tierManager = tierManager;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        String techId = plugin.techniqueManager().getAssignedId(attacker.getUniqueId());
        if (!"wheel".equalsIgnoreCase(techId)) return;
        // Tier-based damage bonus
        int tier = tierManager.getTier(attacker.getUniqueId());
        event.setDamage(event.getDamage() * (1.0 + tier * 0.1));
    }
}
