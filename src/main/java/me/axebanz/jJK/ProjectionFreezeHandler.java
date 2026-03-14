package me.axebanz.jJK;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ProjectionFreezeHandler {

    private static final double NORMAL_SHATTER_DAMAGE = 12.0;
    private static final double BREAKER_SHATTER_DAMAGE = 20.0;
    private static final double BREAKER_KNOCKBACK_H = 2.5;
    private static final double BREAKER_KNOCKBACK_Y = 0.7;
    private static final long IMMUNITY_DURATION_MS = 8000L;

    private final JJKCursedToolsPlugin plugin;
    private final BetterModelBridge betterModel;
    private final ProjectionVisuals visuals;

    private final Map<UUID, Boolean> frozenTargets = new ConcurrentHashMap<>();
    private final Map<UUID, Long> immunityUntil = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> frozenByAttacker = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> frozenIsPlayer = new ConcurrentHashMap<>();
    private final Map<UUID, Location> glassLocationByTarget = new ConcurrentHashMap<>();

    public ProjectionFreezeHandler(JJKCursedToolsPlugin plugin, BetterModelBridge betterModel, ProjectionVisuals visuals) {
        this.plugin = plugin;
        this.betterModel = betterModel;
        this.visuals = visuals;
    }

    public boolean tryPathFreeze(Player attacker, Player target) {
        return tryPathFreezeEntity(attacker, target);
    }

    public boolean tryPathFreezeEntity(Player attacker, LivingEntity target) {
        UUID tId = target.getUniqueId();

        if (tId.equals(attacker.getUniqueId())) return false;
        if (target instanceof Player pTarget && pTarget.isBlocking()) return false;

        Long immunity = immunityUntil.get(tId);
        if (immunity != null && System.currentTimeMillis() < immunity) return false;

        if (isFrozen(tId)) return false;

        applyFreeze(attacker, target);
        return true;
    }

    private void applyFreeze(Player attacker, LivingEntity target) {
        UUID tId = target.getUniqueId();
        frozenTargets.put(tId, true);
        frozenByAttacker.put(tId, attacker.getUniqueId());
        frozenIsPlayer.put(tId, target instanceof Player);

        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 10, false, false, true));
        target.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, Integer.MAX_VALUE, 250, false, false, true));
        target.setVelocity(new Vector(0, 0, 0));

        if (target instanceof org.bukkit.entity.Mob mob) {
            mob.setAI(false);
        }

        // ALWAYS spawn glass as a separate model AT the target's location
        // Never disguise the player — this preserves their skin
        Location freezeLoc = target.getLocation().clone();
        glassLocationByTarget.put(tId, freezeLoc);
        betterModel.spawnModelAt(freezeLoc, "glass");

        target.getWorld().playSound(freezeLoc, Sound.BLOCK_GLASS_PLACE, 1.0f, 0.8f);
        visuals.spawnEntityFrameLock(freezeLoc);

        if (target instanceof Player pTarget) {
            pTarget.sendMessage(plugin.cfg().prefix() + "§9FRAME LOCK! §7You are frozen until shattered!");
        }
        attacker.sendMessage(plugin.cfg().prefix() + "§9Frame Lock §7→ §f" + target.getName());

        plugin.projectionManager().setFrozenTarget(attacker.getUniqueId(), tId);
    }

    private void removeGlass(UUID targetId) {
        Location glassLoc = glassLocationByTarget.remove(targetId);

        // Always remove the glass model at the location — never undisguise a player
        if (glassLoc != null) {
            betterModel.removeGlassAt(glassLoc, 2.0);
        }
    }

    public void normalShatter(Player attacker, UUID targetId) {
        if (!isFrozen(targetId)) return;

        frozenTargets.remove(targetId);
        frozenByAttacker.remove(targetId);
        frozenIsPlayer.remove(targetId);
        immunityUntil.put(targetId, System.currentTimeMillis() + IMMUNITY_DURATION_MS);

        Entity e = Bukkit.getEntity(targetId);
        if (e instanceof LivingEntity le) {
            le.removePotionEffect(PotionEffectType.SLOWNESS);
            le.removePotionEffect(PotionEffectType.JUMP_BOOST);

            if (le instanceof org.bukkit.entity.Mob mob) {
                mob.setAI(true);
            }

            // Play break animation at the glass location, then remove
            Location glassLoc = glassLocationByTarget.get(targetId);
            if (glassLoc != null) {
                betterModel.testAnimation("glass", "break", glassLoc);
            }

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                removeGlass(targetId);
            }, 15L);

            le.damage(NORMAL_SHATTER_DAMAGE, attacker);

            le.getWorld().playSound(le.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.5f, 0.8f);
            visuals.spawnEntityFrameLock(le.getLocation());

            if (le instanceof Player p) {
                p.sendMessage(plugin.cfg().prefix() + "§cFrame shattered! §76 hearts damage.");
            }
            attacker.sendMessage(plugin.cfg().prefix() + "§9Shattered §f" + le.getName() + "§9!");
        }
    }

    public void breakerShatter(Player attacker, UUID targetId) {
        if (!isFrozen(targetId)) return;

        frozenTargets.remove(targetId);
        frozenByAttacker.remove(targetId);
        frozenIsPlayer.remove(targetId);
        immunityUntil.put(targetId, System.currentTimeMillis() + IMMUNITY_DURATION_MS);

        Entity e = Bukkit.getEntity(targetId);
        if (e instanceof LivingEntity le) {
            le.removePotionEffect(PotionEffectType.SLOWNESS);
            le.removePotionEffect(PotionEffectType.JUMP_BOOST);

            if (le instanceof org.bukkit.entity.Mob mob) {
                mob.setAI(true);
            }

            // Play break animation at the glass location, then remove
            Location glassLoc = glassLocationByTarget.get(targetId);
            if (glassLoc != null) {
                betterModel.testAnimation("glass", "break", glassLoc);
            }

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                removeGlass(targetId);
            }, 15L);

            le.damage(BREAKER_SHATTER_DAMAGE, attacker);

            Vector dir = le.getLocation().toVector().subtract(attacker.getLocation().toVector());
            dir.setY(0);
            if (dir.lengthSquared() > 0.01) {
                dir.normalize().multiply(BREAKER_KNOCKBACK_H);
            } else {
                dir = attacker.getLocation().getDirection().clone();
                dir.setY(0);
                if (dir.lengthSquared() > 0.01) {
                    dir.normalize().multiply(BREAKER_KNOCKBACK_H);
                } else {
                    dir = new Vector(1, 0, 0).multiply(BREAKER_KNOCKBACK_H);
                }
            }
            dir.setY(BREAKER_KNOCKBACK_Y);
            le.setVelocity(dir);

            le.getWorld().playSound(le.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.5f, 0.6f);

            le.getWorld().spawnParticle(
                    org.bukkit.Particle.DUST,
                    le.getLocation().clone().add(0, 1, 0),
                    30, 1.0, 1.0, 1.0, 0,
                    new org.bukkit.Particle.DustOptions(org.bukkit.Color.fromRGB(0, 100, 255), 2.0f)
            );
            le.getWorld().spawnParticle(org.bukkit.Particle.ELECTRIC_SPARK,
                    le.getLocation().clone().add(0, 1, 0), 20, 0.8, 0.8, 0.8, 0.05);

            if (le instanceof Player p) {
                p.sendMessage(plugin.cfg().prefix() + "§c§lBREAKER SHATTER! §710 hearts damage!");
            }
            attacker.sendMessage(plugin.cfg().prefix() + "§9§lBREAKER SHATTER §7→ §f" + le.getName() + "§7!");
        }
    }

    public boolean isFrozen(UUID uuid) {
        return frozenTargets.getOrDefault(uuid, false);
    }

    public UUID getFreezer(UUID targetId) {
        return frozenByAttacker.get(targetId);
    }

    public void cleanupPlayer(UUID uuid) {
        if (isFrozen(uuid)) {
            Entity e = Bukkit.getEntity(uuid);
            if (e instanceof LivingEntity le) {
                le.removePotionEffect(PotionEffectType.SLOWNESS);
                le.removePotionEffect(PotionEffectType.JUMP_BOOST);
                if (le instanceof org.bukkit.entity.Mob mob) mob.setAI(true);
            }
            removeGlass(uuid);
        }
        frozenTargets.remove(uuid);
        immunityUntil.remove(uuid);
        frozenByAttacker.remove(uuid);
        frozenIsPlayer.remove(uuid);
        glassLocationByTarget.remove(uuid);
    }
}