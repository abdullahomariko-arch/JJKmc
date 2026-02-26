package me.axebanz.jJK;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class ProjectionListener implements Listener {
    private final JJKCursedToolsPlugin plugin;
    private final ProjectionManager manager;

    public ProjectionListener(JJKCursedToolsPlugin plugin, ProjectionManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        String techId = plugin.techniqueManager().getAssignedId(player.getUniqueId());
        if (!"projection".equalsIgnoreCase(techId)) return;
        // Additional interaction logic can be handled here
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        String techId = plugin.techniqueManager().getAssignedId(attacker.getUniqueId());
        if (!"projection".equalsIgnoreCase(techId)) return;
        // Projection combat logic handled here
    }
}
