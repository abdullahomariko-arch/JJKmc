package me.axebanz.jJK;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Handles séance and permadeath events.
 *
 * Bug Fix #6: On permadeath, kick the player after saving data.
 *             On join, if permaDead, teleport to waiting room and apply effects.
 *             onRespawn handler removed (players get kicked now).
 *
 * Bug Fix #7: onInteractEntity handler removed — cursed body detection now uses
 *             dropped item entities on the ground near armor stands.
 */
public class SeanceListener implements Listener {
    private final JJKCursedToolsPlugin plugin;
    private final SeanceManager seanceManager;
    private final PlayerDataStore dataStore;

    public SeanceListener(JJKCursedToolsPlugin plugin, SeanceManager seanceManager,
                          PlayerDataStore dataStore) {
        this.plugin = plugin;
        this.seanceManager = seanceManager;
        this.dataStore = dataStore;
    }

    /**
     * Bug Fix #6: When permadeath triggers, kick the player after saving data.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (!plugin.cfg().permadeathEnabled()) return;

        PlayerProfile profile = dataStore.getOrCreate(player.getUniqueId());
        if (profile.isPermaDead()) return; // already permadead

        profile.setPermaDead(true);
        dataStore.save(player.getUniqueId());

        // Bug Fix #6: kick the player with permadeath message
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                player.kickPlayer("§5§lYou have fallen in battle.\n§7Your soul awaits reincarnation...");
            }
        }, 1L);
    }

    /**
     * Bug Fix #6: On join, if player is permaDead, teleport to waiting room and apply effects.
     * Do NOT kick them again — they need to be here for séance.
     */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerProfile profile = dataStore.getOrCreate(player.getUniqueId());

        if (profile.isPermaDead()) {
            // Teleport to waiting room
            Location waitingRoom = getWaitingRoom();
            if (waitingRoom != null) {
                player.teleport(waitingRoom);
            }

            // Apply visual effects to indicate permadeath state
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 0, false, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, Integer.MAX_VALUE, 2, false, false));

            player.sendMessage(plugin.cfg().prefix() + "§5§lYou have fallen in battle.");
            player.sendMessage(plugin.cfg().prefix() + "§7Find a practitioner to perform your séance.");
        }
    }

    // Bug Fix #6: onRespawn handler removed — permadead players are kicked, not respawned.

    // Bug Fix #7: onInteractEntity handler removed — cursed body detection is now done
    // by finding dropped Item entities near armor stands (see SeanceManager).

    public void startSeance(Player incantator, ArmorStand stand) {
        seanceManager.startIncantation(incantator, stand);
    }

    private Location getWaitingRoom() {
        FileConfiguration cfg = plugin.getConfig();
        String worldName = cfg.getString("seance.waiting-room.world", "world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        double x = cfg.getDouble("seance.waiting-room.x", 0.5);
        double y = cfg.getDouble("seance.waiting-room.y", 64.0);
        double z = cfg.getDouble("seance.waiting-room.z", 0.5);
        return new Location(world, x, y, z);
    }
}
