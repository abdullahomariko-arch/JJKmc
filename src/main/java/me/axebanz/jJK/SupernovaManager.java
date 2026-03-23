package me.axebanz.jJK;

import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class SupernovaManager {

    private final JJKCursedToolsPlugin plugin;
    private final Map<UUID, List<ArmorStand>> playerOrbs = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> rotationTasks = new ConcurrentHashMap<>();
    private final Map<UUID, Double> orbAngles = new ConcurrentHashMap<>();

    public SupernovaManager(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    /** Spawn a new orb for the player (max 6). Returns true if successful. */
    public boolean activateOrb(Player player) {
        UUID uuid = player.getUniqueId();
        List<ArmorStand> orbs = playerOrbs.computeIfAbsent(uuid, k -> new ArrayList<>());

        if (orbs.size() >= 6) return false;

        Location loc = player.getLocation().clone().add(2, 1, 0);
        ArmorStand stand = player.getWorld().spawn(loc, ArmorStand.class, as -> {
            as.setInvisible(true);
            as.setGravity(false);
            as.setMarker(true);
            as.setCustomNameVisible(false);
            as.setSmall(true);
        });

        orbs.add(stand);

        // Start rotation task if not already running
        if (!rotationTasks.containsKey(uuid)) {
            orbAngles.put(uuid, 0.0);
            BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                if (!player.isOnline()) {
                    stopRotation(uuid);
                    return;
                }
                List<ArmorStand> currentOrbs = playerOrbs.getOrDefault(uuid, new ArrayList<>());
                if (currentOrbs.isEmpty()) {
                    stopRotation(uuid);
                    return;
                }

                double angle = orbAngles.getOrDefault(uuid, 0.0);
                // ~45 deg/sec at 20 tps with 2-tick period
                angle = (angle + Math.toRadians(9)) % (Math.PI * 2);
                orbAngles.put(uuid, angle);

                Location center = player.getLocation().clone().add(0, 1.2, 0);

                // Remove dead orbs before repositioning to avoid ConcurrentModificationException
                currentOrbs.removeIf(ArmorStand::isDead);
                if (currentOrbs.isEmpty()) {
                    stopRotation(uuid);
                    return;
                }

                for (int i = 0; i < currentOrbs.size(); i++) {
                    ArmorStand orb = currentOrbs.get(i);
                    double a = angle + (Math.PI * 2 * i / currentOrbs.size());
                    double x = Math.cos(a) * 2.5;
                    double z = Math.sin(a) * 2.5;
                    Location newLoc = center.clone().add(x, 0, z);
                    orb.teleport(newLoc);

                    Particle.DustOptions red = new Particle.DustOptions(Color.RED, 2.0f);
                    player.getWorld().spawnParticle(Particle.DUST, newLoc, 3, 0.1, 0.1, 0.1, 0, red);
                }
            }, 0L, 2L);
            rotationTasks.put(uuid, task);
        }

        return true;
    }

    /** Explode all orbs, deal 8 damage to nearby entities, then clean up. */
    public void explodeOrbs(Player player) {
        UUID uuid = player.getUniqueId();
        List<ArmorStand> orbs = playerOrbs.remove(uuid);
        stopRotation(uuid);

        if (orbs == null || orbs.isEmpty()) {
            player.sendMessage(plugin.cfg().prefix() + "§4No orbs to explode.");
            return;
        }

        for (ArmorStand orb : orbs) {
            if (orb.isDead()) continue;
            Location loc = orb.getLocation();
            World w = loc.getWorld();
            if (w != null) {
                w.spawnParticle(Particle.EXPLOSION, loc, 3, 0.3, 0.3, 0.3, 0);
                w.spawnParticle(Particle.DUST, loc, 20, 1.0, 1.0, 1.0, 0,
                        new Particle.DustOptions(Color.RED, 2.0f));
                w.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.2f);

                for (LivingEntity le : w.getNearbyLivingEntities(loc, 3.0)) {
                    if (le.getUniqueId().equals(player.getUniqueId())) continue;
                    le.damage(8.0, player);
                }
            }
            orb.remove();
        }
    }

    public int getOrbCount(Player player) {
        List<ArmorStand> orbs = playerOrbs.get(player.getUniqueId());
        return orbs == null ? 0 : orbs.size();
    }

    private void stopRotation(UUID uuid) {
        BukkitTask task = rotationTasks.remove(uuid);
        if (task != null) task.cancel();
        orbAngles.remove(uuid);
    }

    public void cleanupPlayer(UUID uuid) {
        List<ArmorStand> orbs = playerOrbs.remove(uuid);
        if (orbs != null) orbs.forEach(orb -> { if (!orb.isDead()) orb.remove(); });
        stopRotation(uuid);
    }
}
