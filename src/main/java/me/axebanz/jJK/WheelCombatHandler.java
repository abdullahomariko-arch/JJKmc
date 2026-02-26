package me.axebanz.jJK;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class WheelCombatHandler implements Listener {
    private final JJKCursedToolsPlugin plugin;

    public WheelCombatHandler(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        String techId = plugin.techniqueManager().getAssignedId(attacker.getUniqueId());
        if (!"wheel".equalsIgnoreCase(techId)) return;
        // Wheel combat logic
    }
}
