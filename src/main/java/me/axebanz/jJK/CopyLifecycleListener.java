package me.axebanz.jJK;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class CopyLifecycleListener implements Listener {
    private final JJKCursedToolsPlugin plugin;
    private final CopyManager copyManager;

    public CopyLifecycleListener(JJKCursedToolsPlugin plugin, CopyManager copyManager) {
        this.plugin = plugin;
        this.copyManager = copyManager;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        String techId = plugin.techniqueManager().getAssignedId(attacker.getUniqueId());
        if (!"copy".equalsIgnoreCase(techId)) return;
        // Copy lifecycle logic
    }
}
