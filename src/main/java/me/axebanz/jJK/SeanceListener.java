package me.axebanz.jJK;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public final class SeanceListener implements Listener {

    private final JJKCursedToolsPlugin plugin;
    private final SeanceManager seanceManager;

    public SeanceListener(JJKCursedToolsPlugin plugin, SeanceManager seanceManager) {
        this.plugin = plugin;
        this.seanceManager = seanceManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();

        if (e.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_AIR
                && e.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;

        ItemStack inHand = p.getInventory().getItemInMainHand();
        if (!seanceManager.isBindingVow(inHand)) return;

        String assignedId = plugin.techniqueManager().getAssignedId(p.getUniqueId());
        if ("strawdoll".equalsIgnoreCase(assignedId)) return;

        e.setCancelled(true);

        if (seanceManager.hasActiveSeance(p.getUniqueId())) {
            boolean activated = seanceManager.applyBindingVow(p);
            if (activated) {
                inHand.subtract(1);
            }
            return;
        }

        PlayerProfile prof = plugin.data().get(p.getUniqueId());
        if (prof.seanceBindingVowActive) {
            p.sendMessage(plugin.cfg().prefix() + "§7Binding Vow is already active.");
            return;
        }

        prof.seanceBindingVowActive = true;
        plugin.data().save(p.getUniqueId());
        inHand.subtract(1);

        p.sendMessage(plugin.cfg().prefix() + "§5§lBinding Vow activated! §cYou can no longer deal damage.");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player dead = e.getPlayer();
        UUID deadUuid = dead.getUniqueId();
        PlayerProfile prof = plugin.data().get(deadUuid);

        boolean killedByPlayer = dead.getKiller() != null;

        if (prof.isReincarnated) {
            seanceManager.onReincarnatedPlayerDeath(deadUuid);
            prof.isReincarnated = false;
        }

        boolean permadeathTriggered = killedByPlayer && plugin.cfg().permadeathEnabled() && !prof.permaDead;
        if (permadeathTriggered) {
            prof.permaDeadTechniqueId = prof.techniqueId;
            prof.techniqueId = null;
            prof.permaDead = true;
            plugin.data().save(deadUuid);
            e.setDeathMessage(null);

            ItemStack body = plugin.cursedBody().create(deadUuid);
            dead.getWorld().dropItemNaturally(dead.getLocation(), body);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (dead.isOnline()) {
                    seanceManager.banPermadeadPlayer(dead);
                }
            }, 5L);
        }

        if (seanceManager.hasActiveSeance(deadUuid)) {
            seanceManager.onSeanceUserDeath(deadUuid);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!p.isOnline()) return;
            plugin.data().load(p.getUniqueId());
            PlayerProfile prof = plugin.data().get(p.getUniqueId());

            if (prof.isReincarnated && prof.seanceSpawnWorld != null) {
                World world = Bukkit.getWorld(prof.seanceSpawnWorld);
                if (world != null) {
                    Location spawnLoc = new Location(world, prof.seanceSpawnX, prof.seanceSpawnY, prof.seanceSpawnZ);
                    p.teleport(spawnLoc);
                }
                p.setGameMode(GameMode.SURVIVAL);
                p.setHealth(p.getMaxHealth());
                p.setFoodLevel(20);
                p.sendMessage(plugin.cfg().prefix() + "§5§lYou have been reincarnated! Fight wisely — your soul is still bound.");

                prof.seanceSpawnWorld = null;
                prof.seanceSpawnX = 0;
                prof.seanceSpawnY = 0;
                prof.seanceSpawnZ = 0;
                plugin.data().save(p.getUniqueId());
            }

            if (!prof.permaDead && p.getGameMode() == GameMode.ADVENTURE) {
                p.setGameMode(GameMode.SURVIVAL);
            }
        }, 3L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSeanceUserRespawn(PlayerRespawnEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        if (!seanceManager.hasActiveSeance(uuid)) return;
        Bukkit.getScheduler().runTaskLater(plugin, () -> seanceManager.onSeanceUserRespawn(uuid), 5L);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamageByBindingVow(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player attacker)) return;
        PlayerProfile prof = plugin.data().get(attacker.getUniqueId());
        if (!prof.seanceBindingVowActive) return;
        e.setCancelled(true);
    }
}