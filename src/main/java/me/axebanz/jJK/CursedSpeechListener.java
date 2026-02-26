package me.axebanz.jJK;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class CursedSpeechListener implements Listener {
    private final JJKCursedToolsPlugin plugin;
    private final CursedSpeechManager manager;

    public CursedSpeechListener(JJKCursedToolsPlugin plugin, CursedSpeechManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        String techId = plugin.techniqueManager().getAssignedId(attacker.getUniqueId());
        if (!"cursedspeech".equalsIgnoreCase(techId)) return;
        // Cursed speech combat logic
    }
}
