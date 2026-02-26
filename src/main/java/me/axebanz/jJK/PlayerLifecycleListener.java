package me.axebanz.jJK;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerLifecycleListener implements Listener {
    private final JJKCursedToolsPlugin plugin;
    private final PlayerDataStore dataStore;
    private final CursedEnergyManager ceManager;
    private final BossbarUI bossbarUI;

    public PlayerLifecycleListener(JJKCursedToolsPlugin plugin, PlayerDataStore dataStore,
                                   CursedEnergyManager ceManager, BossbarUI bossbarUI) {
        this.plugin = plugin;
        this.dataStore = dataStore;
        this.ceManager = ceManager;
        this.bossbarUI = bossbarUI;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerProfile profile = dataStore.getOrCreate(player.getUniqueId());
        if (profile.getTechnique() != null) {
            plugin.techniqueManager().assign(player.getUniqueId(), profile.getTechnique());
        }
        ceManager.set(player.getUniqueId(), profile.getCursedEnergy());
        bossbarUI.updateCeBar(player, ceManager.get(player.getUniqueId()), ceManager.getMax());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        PlayerProfile profile = dataStore.getOrCreate(player.getUniqueId());
        profile.setTechnique(plugin.techniqueManager().getAssignedId(player.getUniqueId()));
        profile.setCursedEnergy(ceManager.get(player.getUniqueId()));
        dataStore.unload(player.getUniqueId());
        bossbarUI.removeBar(player);
    }
}
