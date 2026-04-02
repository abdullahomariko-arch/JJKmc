package me.axebanz.jJK;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Energy Discharge Technique — highest CE output.
 *
 * Passive  — Enhanced Strikes: 1.5x melee damage + blue particles, 20% damage reduction.
 * Ability 1 — Tracking Beams: chargeable homing beam, up to 2 seconds for full power.
 * Ability 2 — Granite Blast: 3-phase ultimate beam (1s / 3s / 5s charge).
 */
public final class EnergyDischargeManager {

    private final JJKCursedToolsPlugin plugin;

    // Blue DustOptions colors
    private static final Particle.DustOptions BLUE_DENSE  = new Particle.DustOptions(Color.fromRGB(0, 100, 255), 1.5f);
    private static final Particle.DustOptions BLUE_CORE   = new Particle.DustOptions(Color.fromRGB(0, 100, 255), 1.0f);
    private static final Particle.DustOptions BLUE_MID    = new Particle.DustOptions(Color.fromRGB(50, 150, 255), 1.0f);
    private static final Particle.DustOptions BLUE_OUTER  = new Particle.DustOptions(Color.fromRGB(150, 200, 255), 0.8f);
    private static final Particle.DustOptions BLUE_HIT    = new Particle.DustOptions(Color.fromRGB(0, 150, 255), 1.5f);
    private static final Particle.DustOptions BLUE_STRIKE = new Particle.DustOptions(Color.fromRGB(0, 150, 255), 1.5f);

    // Tracking beam charge state: UUID → charge start time (ms)
    private final Map<UUID, Long> trackingChargeStart = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> trackingChargeTasks = new ConcurrentHashMap<>();

    // Granite blast charge state: UUID → charge start time (ms)
    private final Map<UUID, Long> blastChargeStart = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> blastChargeTasks = new ConcurrentHashMap<>();

    // Active blast tasks (so we can cancel on early release / disable)
    private final Map<UUID, BukkitTask> activeBlastTasks = new ConcurrentHashMap<>();

    // Phase 3: players locked in place
    private final Set<UUID> lockedPlayers = ConcurrentHashMap.newKeySet();

    public EnergyDischargeManager(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PASSIVE — applied in EnergyDischargeListener
    // ─────────────────────────────────────────────────────────────────────────

    /** Returns true if the player has Energy Discharge equipped. */
    public boolean hasTechnique(Player p) {
        String id = plugin.techniqueManager().getAssignedId(p.getUniqueId());
        return "energy_discharge".equalsIgnoreCase(id);
    }

    /**
     * Called on a melee hit where the attacker has Energy Discharge.
     * Applies 1.5× damage multiplier and spawns blue particles at hit location.
     */
    public void applyStrikePassive(Player attacker, LivingEntity victim, double baseDamage, EntityDamageByEntityEvent event) {
        event.setDamage(baseDamage * 1.5);
        Location loc = victim.getLocation().add(0, 1, 0);
        World w = attacker.getWorld();
        w.spawnParticle(Particle.DUST, loc, 8, 0.4, 0.4, 0.4, 0, BLUE_STRIKE);
        w.spawnParticle(Particle.END_ROD, loc, 4, 0.3, 0.3, 0.3, 0.05);
    }

    /**
     * Called when a player with Energy Discharge takes damage.
     * Reduces the damage by 20%.
     */
    public void applyDamageReduction(EntityDamageEvent event) {
        event.setDamage(event.getDamage() * 0.80);
    }

    /** Whether a player is currently locked (Phase 3 blast). */
    public boolean isLocked(UUID uuid) {
        return lockedPlayers.contains(uuid);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ABILITY 1 — TRACKING BEAMS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Starts the tracking beam charge.
     * After 2 seconds the beam fires at full charge.
     * Calling this again while charging fires immediately at current charge level.
     */
    public void startTrackingCharge(Player p) {
        if (!hasTechnique(p)) {
            p.sendMessage(plugin.cfg().prefix() + "§cYou don't have §b⚡ Energy Discharge§c equipped.");
            return;
        }
        UUID uuid = p.getUniqueId();

        // If already charging → fire immediately at current charge
        if (trackingChargeStart.containsKey(uuid)) {
            fireTrackingBeam(p, currentTrackingChargePct(uuid));
            return;
        }

        if (plugin.cooldowns().isOnCooldown(uuid, "ed.tracking")) {
            long rem = plugin.cooldowns().remainingSeconds(uuid, "ed.tracking");
            p.sendMessage(plugin.cfg().prefix() + "§cTracking Beam on cooldown: §f" + rem + "s");
            return;
        }

        // Start charge
        trackingChargeStart.put(uuid, System.currentTimeMillis());

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!p.isOnline()) { cancelTrackingCharge(uuid); return; }
            int pct = currentTrackingChargePct(uuid);
            int filled = pct / 10;
            String bar = "§b" + "│".repeat(filled) + "§7" + "│".repeat(10 - filled);
            p.sendActionBar("§b⚡ Tracking Beam: [" + bar + "] " + pct + "%");

            if (pct >= 100) {
                fireTrackingBeam(p, 100);
            }
        }, 0L, 4L); // update every 4 ticks (0.2s)

        trackingChargeTasks.put(uuid, task);
    }

    private int currentTrackingChargePct(UUID uuid) {
        Long start = trackingChargeStart.get(uuid);
        if (start == null) return 0;
        long elapsed = System.currentTimeMillis() - start;
        return (int) Math.min(100, (elapsed / 2000.0) * 100);
    }

    private void cancelTrackingCharge(UUID uuid) {
        trackingChargeStart.remove(uuid);
        BukkitTask t = trackingChargeTasks.remove(uuid);
        if (t != null) t.cancel();
    }

    private void fireTrackingBeam(Player p, int chargePct) {
        UUID uuid = p.getUniqueId();
        cancelTrackingCharge(uuid);

        double range = chargePct >= 100 ? 60 : 30;
        double damage = chargePct >= 100 ? 25 : 10;

        int ceCost = (int) (plugin.ce().max(uuid) * 0.08);
        if (!plugin.ce().tryConsume(uuid, ceCost)) {
            p.sendMessage(plugin.cfg().prefix() + "§cNot enough Cursed Energy.");
            return;
        }

        plugin.cooldowns().setCooldown(uuid, "ed.tracking", 5);
        p.sendActionBar("§b⚡ Tracking Beam: §7Fired!");

        // Find target
        LivingEntity target = findTarget(p, range);
        if (target == null) {
            p.sendMessage(plugin.cfg().prefix() + "§7No target in range.");
            return;
        }

        launchHomingBeam(p, target, range, damage);
    }

    /** Launches a smooth homing beam of blue particles toward the target. */
    private void launchHomingBeam(Player shooter, LivingEntity target, double maxRange, double damage) {
        World world = shooter.getWorld();
        double[] pos = {
                shooter.getEyeLocation().getX(),
                shooter.getEyeLocation().getY(),
                shooter.getEyeLocation().getZ()
        };
        Vector dir = shooter.getEyeLocation().getDirection().normalize().multiply(3.0);
        double[] velocity = {dir.getX(), dir.getY(), dir.getZ()};
        double speed = 3.0;
        double[] distanceTravelled = {0.0};

        BukkitTask[] taskRef = {null};
        taskRef[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!shooter.isOnline() || !target.isValid() || target.isDead()) {
                taskRef[0].cancel();
                return;
            }
            if (distanceTravelled[0] > maxRange) {
                taskRef[0].cancel();
                return;
            }

            // Smooth homing: slightly bend direction toward target center
            Location targetCenter = target.getLocation().add(0, 1, 0);
            double dx = targetCenter.getX() - pos[0];
            double dy = targetCenter.getY() - pos[1];
            double dz = targetCenter.getZ() - pos[2];
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist > 0.01) {
                double factor = 0.15; // How strongly it curves
                velocity[0] += (dx / dist) * factor;
                velocity[1] += (dy / dist) * factor;
                velocity[2] += (dz / dist) * factor;
            }
            // Normalize to constant speed
            double vLen = Math.sqrt(velocity[0] * velocity[0] + velocity[1] * velocity[1] + velocity[2] * velocity[2]);
            if (vLen > 0.01) {
                velocity[0] = (velocity[0] / vLen) * speed;
                velocity[1] = (velocity[1] / vLen) * speed;
                velocity[2] = (velocity[2] / vLen) * speed;
            }

            // Advance position by speed, spawning dense particles every 0.2 blocks
            double stepSize = 0.2;
            int steps = (int) (speed / stepSize);
            boolean hitTarget = false;
            for (int i = 0; i < steps && !hitTarget; i++) {
                pos[0] += velocity[0] * (stepSize / speed);
                pos[1] += velocity[1] * (stepSize / speed);
                pos[2] += velocity[2] * (stepSize / speed);
                distanceTravelled[0] += stepSize;

                Location particleLoc = new Location(world, pos[0], pos[1], pos[2]);
                world.spawnParticle(Particle.DUST, particleLoc, 3, 0.1, 0.1, 0.1, 0, BLUE_CORE);
                world.spawnParticle(Particle.END_ROD, particleLoc, 1, 0.05, 0.05, 0.05, 0.02);

                // Hit detection
                if (particleLoc.distance(targetCenter) < 1.2) {
                    target.damage(damage, shooter);
                    spawnHitExplosion(world, particleLoc);
                    hitTarget = true;
                }
            }
            if (hitTarget) {
                taskRef[0].cancel();
            }
        }, 0L, 1L);
    }

    private void spawnHitExplosion(World world, Location loc) {
        world.spawnParticle(Particle.DUST, loc, 30, 0.8, 0.8, 0.8, 0, BLUE_HIT);
        world.spawnParticle(Particle.END_ROD, loc, 15, 0.6, 0.6, 0.6, 0.1);
        world.spawnParticle(Particle.EXPLOSION, loc, 5, 0.3, 0.3, 0.3, 0.05);
        world.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5f, 1.8f);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ABILITY 2 — GRANITE BLAST (3 PHASES)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Starts Granite Blast charging.
     * Fires based on charge duration:
     *   ≥ 5s → Phase 3 (Maximum Output)
     *   ≥ 3s → Phase 2
     *   ≥ 1s → Phase 1
     *   < 1s → tells player to charge longer
     * Calling again while charging fires immediately.
     */
    public void startBlastCharge(Player p) {
        if (!hasTechnique(p)) {
            p.sendMessage(plugin.cfg().prefix() + "§cYou don't have §b⚡ Energy Discharge§c equipped.");
            return;
        }
        UUID uuid = p.getUniqueId();

        // Already charging → fire at current phase
        if (blastChargeStart.containsKey(uuid)) {
            fireGraniteBlast(p, currentBlastPhase(uuid));
            return;
        }

        // Phase-based cooldown checks
        if (plugin.cooldowns().isOnCooldown(uuid, "ed.blast")) {
            long rem = plugin.cooldowns().remainingSeconds(uuid, "ed.blast");
            p.sendMessage(plugin.cfg().prefix() + "§cGranite Blast on cooldown: §f" + rem + "s");
            return;
        }

        blastChargeStart.put(uuid, System.currentTimeMillis());

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!p.isOnline()) { cancelBlastCharge(uuid); return; }
            long elapsed = System.currentTimeMillis() - blastChargeStart.getOrDefault(uuid, 0L);
            int phase = currentBlastPhase(uuid);

            String phaseStr = phase == 0 ? "§7Charging..." : "§bPhase " + phase;
            String bar = buildChargeBar(elapsed);
            p.sendActionBar("§b⚡ Granite Blast: " + phaseStr + " " + bar);

            // Auto-fire at max charge (5 seconds)
            if (elapsed >= 5000) {
                fireGraniteBlast(p, 3);
            }
        }, 0L, 4L);

        blastChargeTasks.put(uuid, task);
    }

    private int currentBlastPhase(UUID uuid) {
        Long start = blastChargeStart.get(uuid);
        if (start == null) return 0;
        long elapsed = System.currentTimeMillis() - start;
        if (elapsed >= 5000) return 3;
        if (elapsed >= 3000) return 2;
        if (elapsed >= 1000) return 1;
        return 0;
    }

    private String buildChargeBar(long elapsedMs) {
        int filled = (int) Math.min(10, elapsedMs / 500);
        return "[§b" + "│".repeat(filled) + "§7" + "│".repeat(10 - filled) + "§7]";
    }

    private void cancelBlastCharge(UUID uuid) {
        blastChargeStart.remove(uuid);
        BukkitTask t = blastChargeTasks.remove(uuid);
        if (t != null) t.cancel();
    }

    private void fireGraniteBlast(Player p, int phase) {
        UUID uuid = p.getUniqueId();
        cancelBlastCharge(uuid);

        if (phase == 0) {
            p.sendMessage(plugin.cfg().prefix() + "§cCharge longer! (Phase 1 = 1s, Phase 2 = 3s, Phase 3 = 5s)");
            return;
        }

        // CE cost
        double cePct = phase == 1 ? 0.10 : (phase == 2 ? 0.25 : 0.50);
        int ceCost = (int) (plugin.ce().max(uuid) * cePct);
        if (!plugin.ce().tryConsume(uuid, ceCost)) {
            p.sendMessage(plugin.cfg().prefix() + "§cNot enough Cursed Energy.");
            return;
        }

        // Cooldown
        long cooldown = phase == 1 ? 15 : (phase == 2 ? 30 : 60);
        plugin.cooldowns().setCooldown(uuid, "ed.blast", cooldown);

        // Phase 3: announce, lock player
        if (phase == 3) {
            p.sendMessage("§b§l⚡ YOU HAVE REACHED THE HIGHEST CE OUTPUT!");
            p.getWorld().playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0f, 0.5f);
            p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.8f);
            lockedPlayers.add(uuid);
        }

        // Beam parameters per phase
        double range = phase == 1 ? 30 : (phase == 2 ? 50 : 100);
        double damagePerSec = phase == 1 ? 15 : (phase == 2 ? 25 : 50);
        int durationSecs = phase == 1 ? 5 : (phase == 2 ? 8 : 10);
        double halfWidth = phase == 1 ? 0.5 : (phase == 2 ? 1.0 : 2.0);

        launchGraniteBlastBeam(p, phase, range, damagePerSec, durationSecs, halfWidth);
        spawnBeamVisual(p, phase, range, durationSecs * 20);
    }

    private void launchGraniteBlastBeam(Player p, int phase, double range, double damagePerSec, int durationSecs, double halfWidth) {
        UUID uuid = p.getUniqueId();
        World world = p.getWorld();
        long endTime = System.currentTimeMillis() + durationSecs * 1000L;
        Set<UUID> hitThisTick = new HashSet<>();

        // Phase 3: screen shake for nearby players
        if (phase == 3) {
            Bukkit.getScheduler().runTaskTimer(plugin, task -> {
                if (System.currentTimeMillis() >= endTime) { task.cancel(); return; }
                for (Player nearby : world.getPlayers()) {
                    if (nearby.getLocation().distance(p.getLocation()) <= 40) {
                        nearby.sendTitle("", "§b⚡", 0, 5, 5);
                    }
                }
            }, 0L, 10L);
        }

        BukkitTask beamTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!p.isOnline() || System.currentTimeMillis() >= endTime) {
                cleanupBlast(uuid, phase);
                return;
            }

            // Phase 3: keep player frozen
            if (phase == 3 && lockedPlayers.contains(uuid)) {
                p.setVelocity(new Vector(0, 0, 0));
            }

            Location eyeLoc = p.getEyeLocation();
            Vector dir = eyeLoc.getDirection().normalize();
            hitThisTick.clear();

            // Spawn dense beam particles along the ray
            double step = phase == 3 ? 0.05 : 0.1;
            for (double d = 0.5; d <= range; d += step) {
                Location center = eyeLoc.clone().add(dir.clone().multiply(d));

                // Stop if we hit a solid block (but NOT at player's feet)
                if (d > 1.0 && center.getBlock().getType().isSolid()) {
                    if (phase == 3) {
                        // Destroy blocks at the END of the beam
                        center.getBlock().setType(Material.AIR);
                    }
                    break;
                }

                // Phase 1: 3–5 particles per point
                int coreCount = phase == 1 ? 4 : (phase == 2 ? 6 : 12);
                world.spawnParticle(Particle.DUST, center, coreCount, halfWidth * 0.3, halfWidth * 0.3, halfWidth * 0.3, 0, BLUE_CORE);

                // Outer ring / sparkle
                if (phase >= 2) {
                    world.spawnParticle(Particle.DUST, center, coreCount / 2, halfWidth * 0.6, halfWidth * 0.6, halfWidth * 0.6, 0, BLUE_MID);
                }
                if (phase == 3) {
                    world.spawnParticle(Particle.DUST, center, 4, halfWidth * 0.9, halfWidth * 0.9, halfWidth * 0.9, 0, BLUE_OUTER);
                    world.spawnParticle(Particle.END_ROD, center, 2, halfWidth * 0.5, halfWidth * 0.5, halfWidth * 0.5, 0.05);
                }

                // Hit detection — check cylinder of halfWidth radius
                for (Entity ent : world.getNearbyEntities(center, halfWidth, halfWidth, halfWidth)) {
                    if (!(ent instanceof LivingEntity le)) continue;
                    if (ent.getUniqueId().equals(uuid)) continue;
                    if (hitThisTick.contains(ent.getUniqueId())) continue;
                    hitThisTick.add(ent.getUniqueId());
                    // Damage is per second; beam runs at 1 tick intervals, so divide by 20
                    le.damage(damagePerSec / 20.0, p);
                }
            }
        }, 0L, 1L);

        activeBlastTasks.put(uuid, beamTask);
    }

    private void cleanupBlast(UUID uuid, int phase) {
        BukkitTask t = activeBlastTasks.remove(uuid);
        if (t != null) t.cancel();
        if (phase == 3) {
            lockedPlayers.remove(uuid);
        }
        Player p = Bukkit.getPlayer(uuid);
        if (p != null && p.isOnline()) {
            p.sendActionBar("§b⚡ Granite Blast complete.");
        }
    }

    /**
     * Spawns an ItemDisplay beam entity along the player's look direction.
     * Scale Z = beam length, X/Y = thickness (phase-based).
     * Traveling END_ROD particles run for the duration, then the display
     * shrinks X and Y to 0 via interpolation (Red's disappear technique).
     *
     * @param p            the caster
     * @param phase        1, 2, or 3
     * @param maxRange     maximum beam length in blocks
     * @param durationTicks how many ticks before the disappear animation starts
     */
    private void spawnBeamVisual(Player p, int phase, double maxRange, int durationTicks) {
        Location eyeLoc = p.getEyeLocation();
        Vector dir = eyeLoc.getDirection().normalize();
        World world = p.getWorld();

        // Raycast: find where beam hits a solid block
        double beamLength = maxRange;
        for (double d = 0.5; d <= maxRange; d += 0.5) {
            Location step = eyeLoc.clone().add(dir.clone().multiply(d));
            if (step.getBlock().getType().isSolid()) {
                beamLength = d;
                break;
            }
        }

        // Spawn ItemDisplay at beam midpoint
        Location midLoc = eyeLoc.clone().add(dir.clone().multiply(beamLength / 2.0));
        float thickness = switch (phase) {
            case 1 -> 0.8f;
            case 2 -> 1.5f;
            case 3 -> 3.0f;
            default -> 1.0f; // fallback; only phases 1-3 are valid
        };

        ItemDisplay display = (ItemDisplay) world.spawnEntity(midLoc, EntityType.ITEM_DISPLAY);
        display.setItemStack(new ItemStack(Material.BLAZE_ROD)); // placeholder until Nexo model
        display.setRotation(eyeLoc.getYaw(), eyeLoc.getPitch());
        display.setBrightness(new Display.Brightness(15, 15));
        display.setTransformation(new Transformation(
                new Vector3f(0f, 0f, 0f),
                new Quaternionf(),
                new Vector3f(thickness, thickness, (float) beamLength),
                new Quaternionf()
        ));

        // Traveling END_ROD particles along the beam every 2 ticks (use fixed direction from beam creation)
        final double finalBeamLength = beamLength;
        final Location fixedEye = eyeLoc.clone();
        final Vector fixedDir = dir.clone();
        BukkitTask[] particleRef = {null};
        particleRef[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!display.isValid()) {
                particleRef[0].cancel();
                return;
            }
            for (double d = 0; d <= finalBeamLength; d += 2.0) {
                Location sparkLoc = fixedEye.clone().add(fixedDir.clone().multiply(d));
                world.spawnParticle(Particle.END_ROD, sparkLoc, 0,
                        fixedDir.getX() * 0.5, fixedDir.getY() * 0.5, fixedDir.getZ() * 0.5, 0.15);
            }
        }, 0L, 2L);

        // After durationTicks: shrink X,Y to 0 (beam disappear animation)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (particleRef[0] != null) particleRef[0].cancel();
            if (!display.isValid()) return;
            display.setInterpolationDuration(5);
            display.setInterpolationDelay(0);
            Transformation old = display.getTransformation();
            display.setTransformation(new Transformation(
                    old.getTranslation(), old.getLeftRotation(),
                    new Vector3f(0f, 0f, old.getScale().z()),
                    old.getRightRotation()
            ));
            Bukkit.getScheduler().runTaskLater(plugin, display::remove, 7L);
        }, durationTicks);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // COMMAND-BASED ENTRY POINTS
    // ─────────────────────────────────────────────────────────────────────────

    /** Fires the tracking beam via /energydischarge tracking */
    public void cmdTracking(Player p) {
        startTrackingCharge(p);
    }

    /** Fires/charges the granite blast via /energydischarge blast */
    public void cmdBlast(Player p) {
        startBlastCharge(p);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UTILITIES
    // ─────────────────────────────────────────────────────────────────────────

    /** Finds the nearest living entity in range that the player is looking at, or just nearest. */
    private LivingEntity findTarget(Player player, double range) {
        // Try ray-cast to entity the player is looking at
        List<Entity> nearby = new ArrayList<>(player.getNearbyEntities(range, range, range));
        Location eye = player.getEyeLocation();
        Vector dir = eye.getDirection().normalize();

        LivingEntity bestLooking = null;
        double bestDot = 0.7; // minimum cosine angle (≈45°)

        LivingEntity nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (Entity e : nearby) {
            if (!(e instanceof LivingEntity le)) continue;
            if (e.getUniqueId().equals(player.getUniqueId())) continue;
            if (le.isDead()) continue;

            Location entCenter = le.getLocation().add(0, 1, 0);
            double dist = entCenter.distance(eye);
            if (dist > range) continue;

            Vector toEnt = entCenter.toVector().subtract(eye.toVector()).normalize();
            double dot = dir.dot(toEnt);

            if (dot > bestDot) {
                bestDot = dot;
                bestLooking = le;
            }
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = le;
            }
        }

        return bestLooking != null ? bestLooking : nearest;
    }

    /** Cleans up any active tasks for a player (e.g. on logout). */
    public void cleanup(UUID uuid) {
        cancelTrackingCharge(uuid);
        cancelBlastCharge(uuid);
        BukkitTask t = activeBlastTasks.remove(uuid);
        if (t != null) t.cancel();
        lockedPlayers.remove(uuid);
    }
}
