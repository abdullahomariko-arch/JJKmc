package me.axebanz.jJK;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class StrawDollListener implements Listener {
    private final JJKCursedToolsPlugin plugin;
    private final StrawDollManager manager;

    public StrawDollListener(JJKCursedToolsPlugin plugin, StrawDollManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        String techId = plugin.techniqueManager().getAssignedId(attacker.getUniqueId());
        if (!"strawdoll".equalsIgnoreCase(techId)) return;
        // Additional straw doll hit logic can be added here
    }
}
