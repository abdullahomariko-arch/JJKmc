package me.axebanz.jJK;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public final class CursedMobManager implements Listener {

    private final JJKCursedToolsPlugin plugin;
    private final NamespacedKey cursedKey;
    private final Set<UUID> cursedMobs = new HashSet<>();
    private BukkitTask particleTask;

    public CursedMobManager(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
        this.cursedKey = new NamespacedKey(plugin, "cursed_mob");
        startParticleTask();
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL) return;

        Entity e = event.getEntity();
        Random rng = new Random();

        if (e instanceof Creeper creeper && rng.nextInt(10) == 0) {
            makeCursedCreeper(creeper);
        } else if (e instanceof Zombie zombie && rng.nextInt(10) == 0) {
            makeCursedZombie(zombie);
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!cursedMobs.contains(entity.getUniqueId())) return;
        cursedMobs.remove(entity.getUniqueId());

        // Award points to killer if they are a player in culling games
        Player killer = entity.getKiller();
        if (killer != null && plugin.cullingGames() != null) {
            plugin.cullingGames().addPoints(killer.getUniqueId(), 5);
            killer.sendMessage(plugin.cfg().prefix() + "§6+5 points §efor cursed mob kill!");
        }
    }

    private void makeCursedCreeper(Creeper creeper) {
        creeper.setCustomName("§4§lCursed Creeper");
        creeper.setCustomNameVisible(true);
        creeper.setPowered(true);
        creeper.setExplosionRadius(5);

        AttributeInstance maxHp = creeper.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHp != null) {
            maxHp.setBaseValue(40.0);
            creeper.setHealth(40.0);
        }

        creeper.getPersistentDataContainer().set(cursedKey, PersistentDataType.BYTE, (byte) 1);
        cursedMobs.add(creeper.getUniqueId());
    }

    private void makeCursedZombie(Zombie zombie) {
        zombie.setCustomName("§4§lCursed Zombie");
        zombie.setCustomNameVisible(true);

        AttributeInstance maxHp = zombie.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHp != null) {
            maxHp.setBaseValue(50.0);
            zombie.setHealth(50.0);
        }

        AttributeInstance speed = zombie.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (speed != null) speed.setBaseValue(0.35);

        AttributeInstance attack = zombie.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        if (attack != null) attack.setBaseValue(8.0);

        zombie.getPersistentDataContainer().set(cursedKey, PersistentDataType.BYTE, (byte) 1);
        cursedMobs.add(zombie.getUniqueId());
    }

    private void startParticleTask() {
        particleTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Particle.DustOptions red = new Particle.DustOptions(Color.RED, 1.5f);
            cursedMobs.removeIf(uuid -> {
                Entity e = plugin.getServer().getEntity(uuid);
                return e == null || e.isDead();
            });
            for (UUID uuid : new ArrayList<>(cursedMobs)) {
                Entity e = plugin.getServer().getEntity(uuid);
                if (e == null || e.isDead()) continue;
                e.getWorld().spawnParticle(Particle.DUST, e.getLocation().add(0, 1, 0),
                        5, 0.3, 0.5, 0.3, 0, red);
            }
        }, 0L, 10L);
    }

    public boolean isCursedMob(Entity e) {
        return cursedMobs.contains(e.getUniqueId());
    }
}
