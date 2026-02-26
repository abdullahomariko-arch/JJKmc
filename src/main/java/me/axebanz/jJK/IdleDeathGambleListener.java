package me.axebanz.jJK;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class IdleDeathGambleListener implements Listener {
    private final JJKCursedToolsPlugin plugin;
    private final IdleDeathGambleManager manager;

    public IdleDeathGambleListener(JJKCursedToolsPlugin plugin, IdleDeathGambleManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        String techId = plugin.techniqueManager().getAssignedId(attacker.getUniqueId());
        if (!"idle_death_gamble".equalsIgnoreCase(techId)) return;
        // IDG combat logic handled here
    }
}
