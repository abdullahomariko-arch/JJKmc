package me.axebanz.jJK;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all Limitless Technique abilities:
 * Infinity, Blue, Blue Max, Red, Red Max, Hollow Purple, Hollow Purple Nuke,
 * and Domain Expansion: Infinite Void.
 */
public final class LimitlessManager {

    private final JJKCursedToolsPlugin plugin;

    // Infinity state
    private final Map<UUID, BukkitTask> infinityParticleTasks = new ConcurrentHashMap<>();

    // Blue orbs (normal) — uuid -> active ItemDisplay entity
    private final Map<UUID, ItemDisplay> activeBlueOrbs = new ConcurrentHashMap<>();

    // Blue Max orbs — uuid -> active ItemDisplay entity (during travel + active phase)
    private final Map<UUID, ItemDisplay> activeBlueMaxOrbs = new ConcurrentHashMap<>();
    // Blue Max orb active-phase tasks (the pull/damage loop)
    private final Map<UUID, BukkitTask> blueMaxActiveTasks = new ConcurrentHashMap<>();
    // Players who killed something during Blue Max orb — can now lock the orb
    private final Set<UUID> canLockBlue = Collections.newSetFromMap(new ConcurrentHashMap<>());
    // Locked Blue orbs — uuid -> locked ItemDisplay entity (for Nuke detection)
    private final Map<UUID, ItemDisplay> lockedBlueOrbs = new ConcurrentHashMap<>();

    public LimitlessManager(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    // ===== CE cost helpers (percentage-based) =====

    /** Returns CE cost as a percentage of the player's max CE. */
    private int percentCost(Player p, double pct) {
        int maxCe = plugin.ce().max(p.getUniqueId());
        return Math.max(1, (int) Math.round(maxCe * pct));
    }

    /** Returns 0 for Six Eyes holders, otherwise the percentage cost. */
    private int effectiveCost(Player p, double pct) {
        if (hasSixEyes(p)) return 0;
        return percentCost(p, pct);
    }

    // ===== Helpers =====

    private boolean hasTechnique(Player p) {
        String id = plugin.techniqueManager().getAssignedId(p.getUniqueId());
        return "limitless".equalsIgnoreCase(id);
    }

    private boolean hasSixEyes(Player p) {
        return plugin.sixEyes() != null && plugin.sixEyes().hasSixEyes(p);
    }

    private boolean checkTechnique(Player p) {
        if (!hasTechnique(p)) {
            p.sendMessage(plugin.cfg().prefix() + "§cYou don't have the §bLimitless §ctechnique.");
            return false;
        }
        return true;
    }

    private boolean checkCooldown(Player p, String key, long seconds) {
        if (plugin.cooldowns().isOnCooldown(p.getUniqueId(), key)) {
            long rem = plugin.cooldowns().remainingSeconds(p.getUniqueId(), key);
            p.sendMessage(plugin.cfg().prefix() + "§cOn cooldown: §f" + rem + "s");
            return false;
        }
        return true;
    }

    // ===== INFINITY TOGGLE =====

    public void toggleInfinity(Player p) {
        if (!checkTechnique(p)) return;

        UUID uuid = p.getUniqueId();
        PlayerProfile prof = plugin.data().get(uuid);

        if (prof.limitlessInfinityActive) {
            // Deactivate
            prof.limitlessInfinityActive = false;
            plugin.data().save(uuid);
            BukkitTask task = infinityParticleTasks.remove(uuid);
            if (task != null) task.cancel();
            p.sendMessage(plugin.cfg().prefix() + "§bInfinity §7deactivated.");
            p.playSound(p.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.8f, 1.2f);
        } else {
            // Activate — drain CE slowly while active, check threshold
            int cost = effectiveCost(p, 0); // activation is free, drain is periodic
            prof.limitlessInfinityActive = true;
            plugin.data().save(uuid);

            // Particle task — subtle purple/blue swirl
            BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                if (!p.isOnline() || !plugin.data().get(uuid).limitlessInfinityActive) return;

                // Drain CE slowly (1 CE every 60 seconds if not Six Eyes)
                // handled separately via a drain task
                spawnInfinityParticles(p);
            }, 2L, 2L);
            infinityParticleTasks.put(uuid, task);

            p.sendMessage(plugin.cfg().prefix() + "§bInfinity §aactivated! §7All attacks are nullified.");
            p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.8f);
        }
    }

    private void spawnInfinityParticles(Player p) {
        Location loc = p.getLocation().add(0, 1, 0);
        World w = loc.getWorld();
        if (w == null) return;

        double t = (System.currentTimeMillis() % 3000) / 3000.0 * Math.PI * 2;
        for (int i = 0; i < 8; i++) {
            double angle = t + (i * Math.PI * 2.0 / 8);
            double x = Math.cos(angle) * 0.8;
            double z = Math.sin(angle) * 0.8;
            Location point = loc.clone().add(x, 0, z);

            Particle.DustOptions blue = new Particle.DustOptions(Color.fromRGB(30, 80, 255), 0.8f);
            Particle.DustOptions purple = new Particle.DustOptions(Color.fromRGB(120, 0, 200), 0.8f);
            w.spawnParticle(Particle.DUST, point, 1, 0, 0, 0, 0, i % 2 == 0 ? blue : purple);
        }
    }

    public boolean isInfinityActive(Player p) {
        return plugin.data().get(p.getUniqueId()).limitlessInfinityActive;
    }

    public void deactivateInfinityOnDeath(UUID uuid) {
        PlayerProfile prof = plugin.data().get(uuid);
        prof.limitlessInfinityActive = false;
        BukkitTask task = infinityParticleTasks.remove(uuid);
        if (task != null) task.cancel();
    }

    // ===== BLUE (Attraction Orb) =====

    public void castBlue(Player p) {
        if (!checkTechnique(p)) return;
        if (!checkCooldown(p, "limitless_blue", 10)) return;

        int cost = effectiveCost(p, 0.15);
        if (!plugin.ce().tryConsume(p.getUniqueId(), cost)) {
            p.sendMessage(plugin.cfg().prefix() + "§cNot enough Cursed Energy.");
            return;
        }
        plugin.cooldowns().setCooldown(p.getUniqueId(), "limitless_blue", 10);

        p.sendMessage(plugin.cfg().prefix() + "§bBlue §7— attraction orb fired!");
        p.playSound(p.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 0.8f, 1.8f);

        // Raycast: 0.5-block steps, max 30 blocks
        Location eye = p.getEyeLocation();
        Vector dir = eye.getDirection().normalize();
        int totalSteps = 0;
        for (int i = 1; i <= 60; i++) {
            Location check = eye.clone().add(dir.clone().multiply(i * 0.5));
            if (check.getBlock().getType().isSolid()) break;
            totalSteps = i;
        }
        if (totalSteps == 0) totalSteps = 1;

        // Spawn ItemDisplay orb at eye location
        final int finalSteps = totalSteps;
        World world = p.getWorld();
        ItemDisplay orb = world.spawn(eye.clone(), ItemDisplay.class, e -> {
            e.setItemStack(new ItemStack(Material.BLUE_ICE));
            e.setTransformation(new Transformation(
                    new Vector3f(0f, 0f, 0f),
                    new Quaternionf(),
                    new Vector3f(0.5f, 0.5f, 0.5f),
                    new Quaternionf()));
            e.setBrightness(new Display.Brightness(15, 15));
        });
        activeBlueOrbs.put(p.getUniqueId(), orb);

        // Travel along raycast path (1 tick per step)
        final Location fixedEye = eye.clone();
        final int[] step = {0};
        BukkitTask[] travelRef = {null};
        travelRef[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!orb.isValid()) { travelRef[0].cancel(); return; }
            step[0]++;
            Location pos = fixedEye.clone().add(dir.clone().multiply(step[0] * 0.5));
            orb.teleport(pos);
            world.spawnParticle(Particle.SOUL_FIRE_FLAME, pos, 3, 0.1, 0.1, 0.1, 0.02);

            if (step[0] >= finalSteps) {
                travelRef[0].cancel();
                // Orb arrived — active phase for 5 seconds (100 ticks)
                activateBlueOrb(p, orb, 5.0, 100, 0.15, false);
            }
        }, 0L, 1L);
    }

    /** Runs the active pull/damage loop once the Blue orb has arrived. */
    private void activateBlueOrb(Player caster, ItemDisplay orb, double pullRadius,
                                  int durationTicks, double damagePerTick, boolean isMax) {
        UUID uuid = caster.getUniqueId();
        final int[] ticksLeft = {durationTicks};

        BukkitTask[] activeRef = {null};
        activeRef[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!orb.isValid()) {
                activeRef[0].cancel();
                cleanupBlueOrb(uuid, isMax);
                return;
            }

            Location center = orb.getLocation();
            World cw = center.getWorld();
            if (cw == null) return;
            cw.spawnParticle(Particle.SOUL_FIRE_FLAME, center, 4, 0.3, 0.3, 0.3, 0.01);

            // Pull entities toward center, extra damage for very close entities
            for (LivingEntity le : getEntitiesInRadius(center, pullRadius, caster)) {
                Vector pull = center.toVector().subtract(le.getLocation().toVector());
                double dist = pull.length();
                if (dist > 0.1) {
                    pull = pull.normalize().multiply(Math.min(1.5, dist * 0.3));
                    le.setVelocity(le.getVelocity().add(pull));
                }
                if (dist <= 1.0) {
                    le.damage(damagePerTick, caster);
                }
            }

            if (isMax) {
                // Safety: if orb was locked externally, cancel this task
                if (lockedBlueOrbs.containsKey(uuid)) {
                    activeRef[0].cancel();
                    return;
                }
            }

            ticksLeft[0]--;
            if (ticksLeft[0] <= 0) {
                activeRef[0].cancel();
                if (orb.isValid()) orb.remove();
                cleanupBlueOrb(uuid, isMax);
            }
        }, 0L, 1L);

        if (isMax) blueMaxActiveTasks.put(uuid, activeRef[0]);
    }

    private void cleanupBlueOrb(UUID uuid, boolean isMax) {
        activeBlueOrbs.remove(uuid);
        if (isMax) {
            activeBlueMaxOrbs.remove(uuid);
            blueMaxActiveTasks.remove(uuid);
        }
    }

    // ===== BLUE MAX (Maximum Output) =====

    public void castBlueMax(Player p) {
        if (!checkTechnique(p)) return;
        if (!checkCooldown(p, "limitless_blue_max", 25)) return;

        int cost = effectiveCost(p, 0.30);
        if (!plugin.ce().tryConsume(p.getUniqueId(), cost)) {
            p.sendMessage(plugin.cfg().prefix() + "§cNot enough Cursed Energy.");
            return;
        }
        plugin.cooldowns().setCooldown(p.getUniqueId(), "limitless_blue_max", 25);

        p.sendMessage(plugin.cfg().prefix() + "§bBlue Maximum Output §7— attraction orb fired!");
        p.playSound(p.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 0.8f, 2.0f);

        // Raycast: 0.5-block steps, max 30 blocks
        Location eye = p.getEyeLocation();
        Vector dir = eye.getDirection().normalize();
        int totalSteps = 0;
        for (int i = 1; i <= 60; i++) {
            Location check = eye.clone().add(dir.clone().multiply(i * 0.5));
            if (check.getBlock().getType().isSolid()) break;
            totalSteps = i;
        }
        if (totalSteps == 0) totalSteps = 1;

        final int finalSteps = totalSteps;
        World world = p.getWorld();

        // Spawn larger ItemDisplay orb (2.5x normal scale)
        ItemDisplay orb = world.spawn(eye.clone(), ItemDisplay.class, e -> {
            e.setItemStack(new ItemStack(Material.BLUE_ICE));
            e.setTransformation(new Transformation(
                    new Vector3f(0f, 0f, 0f),
                    new Quaternionf(),
                    new Vector3f(1.25f, 1.25f, 1.25f), // 2.5x larger than normal Blue (0.5 base → 1.25 = 2.5× multiplier)
                    new Quaternionf()));
            e.setBrightness(new Display.Brightness(15, 15));
        });
        activeBlueMaxOrbs.put(p.getUniqueId(), orb);

        // Travel along raycast path, destroying blocks in 2-block radius
        final Location fixedEye = eye.clone();
        final int[] step = {0};
        BukkitTask[] travelRef = {null};
        travelRef[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!orb.isValid()) { travelRef[0].cancel(); return; }
            step[0]++;
            Location pos = fixedEye.clone().add(dir.clone().multiply(step[0] * 0.5));
            orb.teleport(pos);
            world.spawnParticle(Particle.SOUL_FIRE_FLAME, pos, 5, 0.2, 0.2, 0.2, 0.02);

            // Destroy blocks in 2-block radius as orb travels (every 4 ticks to reduce lag)
            if (step[0] % 4 == 0) {
                destroyBlocksInRadius(pos, 2, p);
            }

            if (step[0] >= finalSteps) {
                travelRef[0].cancel();
                // Orb arrived — active phase (5 seconds = 100 ticks, pull 8 blocks, 6 HP/s = 0.3 per tick)
                activateBlueOrb(p, orb, 8.0, 100, 0.3, true);
            }
        }, 0L, 1L);
    }

    // ===== Blue Max Lock / Unlock (called by LimitlessListener) =====

    /** Returns true if the player has an active Blue Max orb that can be locked. */
    public boolean hasActiveBlueMaxOrb(Player p) {
        return activeBlueMaxOrbs.containsKey(p.getUniqueId());
    }

    /** Marks the player's Blue Max orb as lockable (called when they kill something). */
    public void markCanLockBlue(Player p) {
        canLockBlue.add(p.getUniqueId());
    }

    /** Returns true if the player can lock their Blue Max orb. */
    public boolean canLockBlue(Player p) {
        return canLockBlue.contains(p.getUniqueId());
    }

    /**
     * Locks the Blue Max orb in place — cancels the active pull task and stores
     * the orb reference for Nuke detection.
     */
    public void lockBlueOrb(Player p) {
        UUID uuid = p.getUniqueId();
        ItemDisplay orb = activeBlueMaxOrbs.get(uuid);
        if (orb == null || !orb.isValid()) return;
        lockedBlueOrbs.put(uuid, orb);
        canLockBlue.remove(uuid);
        // Cancel the active phase task — the orb now persists until released
        BukkitTask task = blueMaxActiveTasks.remove(uuid);
        if (task != null) task.cancel();
        p.sendMessage(plugin.cfg().prefix() + "§b§lBlue orb LOCKED §7— stop sneaking to release.");
    }

    /** Returns true if the player has a locked Blue Max orb. */
    public boolean hasLockedBlueOrb(Player p) {
        UUID uuid = p.getUniqueId();
        ItemDisplay orb = lockedBlueOrbs.get(uuid);
        if (orb == null || !orb.isValid()) {
            lockedBlueOrbs.remove(uuid);
            return false;
        }
        return true;
    }

    /** Removes the locked Blue orb. */
    public void removeLockedBlueOrb(Player p) {
        UUID uuid = p.getUniqueId();
        ItemDisplay orb = lockedBlueOrbs.remove(uuid);
        if (orb != null && orb.isValid()) orb.remove();
        activeBlueMaxOrbs.remove(uuid);
        BukkitTask task = blueMaxActiveTasks.remove(uuid);
        if (task != null) task.cancel();
        p.sendMessage(plugin.cfg().prefix() + "§7Blue orb released.");
    }

    // ===== RED (Repulsion Orb — requires RCT) =====

    public void castRed(Player p) {
        if (!checkTechnique(p)) return;

        // Requires RCT — Six Eyes trait OR CE level >= 200
        boolean hasRct = plugin.ce().hasRct(p.getUniqueId());
        if (!hasRct) {
            p.sendMessage(plugin.cfg().prefix() + "§cYou need Reverse Cursed Technique to use Red. §7(Need Six Eyes or 200 CE level)");
            return;
        }
        if (!checkCooldown(p, "limitless_red", 15)) return;

        int cost = effectiveCost(p, 0.20);
        if (!plugin.ce().tryConsume(p.getUniqueId(), cost)) {
            p.sendMessage(plugin.cfg().prefix() + "§cNot enough Cursed Energy.");
            return;
        }
        plugin.cooldowns().setCooldown(p.getUniqueId(), "limitless_red", 15);

        p.sendMessage(plugin.cfg().prefix() + "§cRed §7— repulsion orb fired!");
        p.playSound(p.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 0.8f, 0.7f);

        // Raycast: 0.5-block steps, max 30 blocks
        Location eye = p.getEyeLocation();
        Vector dir = eye.getDirection().normalize();
        int totalSteps = 0;
        for (int i = 1; i <= 60; i++) {
            Location check = eye.clone().add(dir.clone().multiply(i * 0.5));
            if (check.getBlock().getType().isSolid()) break;
            totalSteps = i;
        }
        if (totalSteps == 0) totalSteps = 1;

        final int finalSteps = totalSteps;
        World world = p.getWorld();

        // Spawn red ItemDisplay orb
        ItemDisplay orb = world.spawn(eye.clone(), ItemDisplay.class, e -> {
            e.setItemStack(new ItemStack(Material.NETHER_BRICK));
            e.setTransformation(new Transformation(
                    new Vector3f(0f, 0f, 0f),
                    new Quaternionf(),
                    new Vector3f(0.5f, 0.5f, 0.5f),
                    new Quaternionf()));
            e.setBrightness(new Display.Brightness(15, 15));
        });

        final Location fixedEye = eye.clone();
        final int[] step = {0};
        BukkitTask[] travelRef = {null};
        travelRef[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!orb.isValid()) { travelRef[0].cancel(); return; }
            step[0]++;
            Location pos = fixedEye.clone().add(dir.clone().multiply(step[0] * 0.5));
            orb.teleport(pos);
            world.spawnParticle(Particle.FLAME, pos, 3, 0.1, 0.1, 0.1, 0.02);

            if (step[0] >= finalSteps) {
                travelRef[0].cancel();
                // Orb arrived — push entities within 5 blocks, 10 HP + strong knockback
                Location center = pos.clone();
                for (LivingEntity le : getEntitiesInRadius(center, 5.0, p)) {
                    Vector push = le.getLocation().toVector().subtract(center.toVector());
                    if (push.length() < 0.1) push = new Vector(1, 0.5, 0);
                    else push = push.normalize().multiply(2.5).add(new Vector(0, 0.5, 0));
                    le.setVelocity(push);
                    le.damage(10.0, p);
                }
                world.spawnParticle(Particle.EXPLOSION, center, 3, 0.5, 0.5, 0.5, 0);
                world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1.4f);

                // Brief visual then remove
                final int[] visTicks = {30};
                BukkitTask[] visRef = {null};
                visRef[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                    if (!orb.isValid()) { visRef[0].cancel(); return; }
                    world.spawnParticle(Particle.FLAME, orb.getLocation(), 5, 0.3, 0.3, 0.3, 0.02);
                    visTicks[0]--;
                    if (visTicks[0] <= 0) {
                        visRef[0].cancel();
                        if (orb.isValid()) orb.remove();
                    }
                }, 0L, 1L);
            }
        }, 0L, 1L);
    }

    // ===== RED MAX (Destruction Beam — requires RCT) =====

    public void castRedMax(Player p) {
        if (!checkTechnique(p)) return;

        if (!plugin.ce().hasRct(p.getUniqueId())) {
            p.sendMessage(plugin.cfg().prefix() + "§cRed Max requires §aReverse Cursed Technique§c!");
            return;
        }
        if (!checkCooldown(p, "limitless_red_max", 30)) return;

        int cost = effectiveCost(p, 0.40);
        if (!plugin.ce().tryConsume(p.getUniqueId(), cost)) {
            p.sendMessage(plugin.cfg().prefix() + "§cNot enough Cursed Energy.");
            return;
        }
        plugin.cooldowns().setCooldown(p.getUniqueId(), "limitless_red_max", 30);

        p.sendMessage(plugin.cfg().prefix() + "§cRed Maximum Output §7— destruction beam!");
        p.playSound(p.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.5f);

        Location eyeLoc = p.getEyeLocation();
        Vector dir = eyeLoc.getDirection().normalize();
        World world = p.getWorld();

        // Raycast: find beam length (max 60 blocks), destroy 1-block radius along beam
        double beamLength = 60.0;
        for (double d = 0.5; d <= 60.0; d += 0.5) {
            Location step = eyeLoc.clone().add(dir.clone().multiply(d));
            if (step.getBlock().getType().isSolid()) {
                beamLength = d;
                break;
            }
            // Destroy blocks in 1-block radius
            destroyBlocksInRadius(step, 1, p);
        }

        // Damage all entities within 2 blocks of beam line
        final double finalBeamLength = beamLength;
        for (double d = 0; d <= finalBeamLength; d += 0.5) {
            Location beamPt = eyeLoc.clone().add(dir.clone().multiply(d));
            for (LivingEntity le : getEntitiesInRadius(beamPt, 2.0, p)) {
                le.damage(20.0, p);
            }
        }

        // Spawn ItemDisplay beam at midpoint, Z scale = beam length
        Location midLoc = eyeLoc.clone().add(dir.clone().multiply(finalBeamLength / 2.0));
        ItemDisplay display = world.spawn(midLoc, ItemDisplay.class, e -> {
            e.setItemStack(new ItemStack(Material.NETHER_BRICK));
            e.setTransformation(new Transformation(
                    new Vector3f(0f, 0f, 0f),
                    new Quaternionf(),
                    new Vector3f(0.5f, 0.5f, (float) finalBeamLength),
                    new Quaternionf()));
            e.setRotation(eyeLoc.getYaw(), eyeLoc.getPitch());
            e.setBrightness(new Display.Brightness(15, 15));
        });

        // END_ROD traveling particles along beam
        final Location fixedEye = eyeLoc.clone();
        final Vector fixedDir = dir.clone();
        BukkitTask[] particleRef = {null};
        particleRef[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!display.isValid()) { particleRef[0].cancel(); return; }
            for (double d = 0; d <= finalBeamLength; d += 2.0) {
                Location sparkLoc = fixedEye.clone().add(fixedDir.clone().multiply(d));
                world.spawnParticle(Particle.END_ROD, sparkLoc, 0,
                        fixedDir.getX() * 0.5, fixedDir.getY() * 0.5, fixedDir.getZ() * 0.5, 0.15);
            }
        }, 0L, 2L);

        // After 1 second (20 ticks): disappear animation (X, Y → 0 over 5 ticks)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (particleRef[0] != null) particleRef[0].cancel();
            if (!display.isValid()) return;
            display.setInterpolationDuration(5);
            display.setInterpolationDelay(0);
            Transformation old = display.getTransformation();
            display.setTransformation(new Transformation(
                    old.getTranslation(), old.getLeftRotation(),
                    new Vector3f(0f, 0f, old.getScale().z()),
                    old.getRightRotation()));
            Bukkit.getScheduler().runTaskLater(plugin, display::remove, 7L);
        }, 20L);
    }

    // ===== HOLLOW PURPLE (Beam — requires RCT) =====

    public void castHollowPurple(Player p) {
        if (!checkTechnique(p)) return;

        if (!plugin.ce().hasRct(p.getUniqueId())) {
            p.sendMessage(plugin.cfg().prefix() + "§cHollow Purple requires §aReverse Cursed Technique§c!");
            return;
        }
        if (!checkCooldown(p, "limitless_purple", 60)) return;

        int cost = effectiveCost(p, 0.50);
        if (!plugin.ce().tryConsume(p.getUniqueId(), cost)) {
            p.sendMessage(plugin.cfg().prefix() + "§cNot enough Cursed Energy.");
            return;
        }
        plugin.cooldowns().setCooldown(p.getUniqueId(), "limitless_purple", 60);

        p.sendMessage(plugin.cfg().prefix() + "§5Hollow Purple §7— fired!");
        p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.5f);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 1.2f);

        Location eye = p.getEyeLocation();
        Vector dir = eye.getDirection().normalize();
        World world = p.getWorld();

        // Raycast: max 100 blocks, destroy 3-block radius cylinder along entire path
        double beamLength = 100.0;
        for (double d = 0.5; d <= 100.0; d += 0.5) {
            Location step = eye.clone().add(dir.clone().multiply(d));
            if (step.getBlock().getType().isSolid()) {
                beamLength = d;
                break;
            }
        }

        // Destroy blocks in 3-block radius cylinder and damage entities
        final double finalBeamLength = beamLength;
        Set<UUID> hit = new HashSet<>();
        for (double d = 0; d <= finalBeamLength; d += 0.5) {
            Location beamPt = eye.clone().add(dir.clone().multiply(d));
            destroyBlocksInRadius(beamPt, 3, p);
            // Damage entities within 3 blocks
            for (LivingEntity le : getEntitiesInRadius(beamPt, 3.0, p)) {
                if (!hit.contains(le.getUniqueId())) {
                    hit.add(le.getUniqueId());
                    le.damage(40.0, p);
                }
            }
        }

        // Spawn ItemDisplay beam, Z scale = beam length
        Location midLoc = eye.clone().add(dir.clone().multiply(finalBeamLength / 2.0));
        ItemDisplay display = world.spawn(midLoc, ItemDisplay.class, e -> {
            e.setItemStack(new ItemStack(Material.CRYING_OBSIDIAN));
            e.setTransformation(new Transformation(
                    new Vector3f(0f, 0f, 0f),
                    new Quaternionf(),
                    new Vector3f(1.0f, 1.0f, (float) finalBeamLength),
                    new Quaternionf()));
            e.setRotation(eye.getYaw(), eye.getPitch());
            e.setBrightness(new Display.Brightness(15, 15));
        });

        // DRAGON_BREATH + END_ROD particles along the beam
        final Location fixedEye = eye.clone();
        final Vector fixedDir = dir.clone();
        BukkitTask[] particleRef = {null};
        particleRef[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!display.isValid()) { particleRef[0].cancel(); return; }
            for (double d = 0; d <= finalBeamLength; d += 3.0) {
                Location sparkLoc = fixedEye.clone().add(fixedDir.clone().multiply(d));
                world.spawnParticle(Particle.DRAGON_BREATH, sparkLoc, 3, 0.3, 0.3, 0.3, 0.01);
                world.spawnParticle(Particle.END_ROD, sparkLoc, 0,
                        fixedDir.getX() * 0.5, fixedDir.getY() * 0.5, fixedDir.getZ() * 0.5, 0.15);
            }
        }, 0L, 2L);

        // Remove after 1.5 seconds
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (particleRef[0] != null) particleRef[0].cancel();
            if (display.isValid()) display.remove();
        }, 30L);
    }

    // ===== HOLLOW NUKE =====

    public void castNuke(Player p) {
        if (!checkTechnique(p)) return;

        // Need a locked Blue orb to target
        UUID uuid = p.getUniqueId();
        ItemDisplay lockedOrb = lockedBlueOrbs.get(uuid);
        if (lockedOrb == null || !lockedOrb.isValid()) {
            p.sendMessage(plugin.cfg().prefix() + "§cNo locked Blue orb. Use §bBlue Max§c, kill something, then Shift to lock it.");
            return;
        }

        // CE cost: 0 (already paid for Blue Max + Red implicitly)
        p.sendMessage(plugin.cfg().prefix() + "§5§lHOLLOW PURPLE NUKE §7— Red orb incoming!");

        // Fire a Red orb toward the locked Blue orb
        Location eye = p.getEyeLocation();
        Location bluePos = lockedOrb.getLocation().clone();
        Vector toward = bluePos.toVector().subtract(eye.toVector()).normalize();
        World world = p.getWorld();

        // Spawn Red orb at eye location
        ItemDisplay redOrb = world.spawn(eye.clone(), ItemDisplay.class, e -> {
            e.setItemStack(new ItemStack(Material.NETHER_BRICK));
            e.setTransformation(new Transformation(
                    new Vector3f(0f, 0f, 0f),
                    new Quaternionf(),
                    new Vector3f(0.6f, 0.6f, 0.6f),
                    new Quaternionf()));
            e.setBrightness(new Display.Brightness(15, 15));
        });

        final int[] step = {0};
        final double distance = eye.distance(bluePos);
        final int maxSteps = (int) (distance / 0.5) + 10;
        BukkitTask[] travelRef = {null};
        travelRef[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!redOrb.isValid()) { travelRef[0].cancel(); return; }
            step[0]++;
            Location pos = eye.clone().add(toward.clone().multiply(step[0] * 0.5));
            redOrb.teleport(pos);
            world.spawnParticle(Particle.FLAME, pos, 3, 0.1, 0.1, 0.1, 0.02);

            // Check collision with locked Blue orb (within 2 blocks)
            ItemDisplay currentBlue = lockedBlueOrbs.get(uuid);
            if (currentBlue != null && currentBlue.isValid() && pos.distance(currentBlue.getLocation()) < 2.0) {
                travelRef[0].cancel();
                if (redOrb.isValid()) redOrb.remove();
                triggerNuke(p, currentBlue.getLocation().clone());
                return;
            }

            if (step[0] >= maxSteps) {
                travelRef[0].cancel();
                if (redOrb.isValid()) redOrb.remove();
            }
        }, 0L, 1L);
    }

    private void triggerNuke(Player caster, Location center) {
        UUID uuid = caster.getUniqueId();

        // Remove locked Blue orb
        ItemDisplay blueOrb = lockedBlueOrbs.remove(uuid);
        if (blueOrb != null && blueOrb.isValid()) blueOrb.remove();
        activeBlueMaxOrbs.remove(uuid);
        BukkitTask task = blueMaxActiveTasks.remove(uuid);
        if (task != null) task.cancel();
        canLockBlue.remove(uuid);

        World w = center.getWorld();
        if (w == null) return;

        // Screen shake: send rapid title packets to all players within 50 blocks
        for (Player nearby : w.getPlayers()) {
            if (nearby.getLocation().distance(center) <= 50) {
                // Rapid title flashes simulate screen shake
                final int[] shakeCount = {0};
                BukkitTask[] shakeRef = {null};
                shakeRef[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                    shakeCount[0]++;
                    if (shakeCount[0] > 10) {
                        shakeRef[0].cancel();
                        nearby.resetTitle();
                        return;
                    }
                    nearby.sendTitle("§5§l✦", "§7", 0, 2, 0);
                }, 0L, 1L);
            }
        }

        // Sounds
        w.playSound(center, Sound.ENTITY_WITHER_SPAWN, 4.0f, 0.3f);
        w.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 4.0f, 0.3f);

        // Particles
        w.spawnParticle(Particle.EXPLOSION, center, 20, 10, 10, 10, 0);
        Particle.DustOptions purple = new Particle.DustOptions(Color.fromRGB(160, 0, 200), 3.0f);
        Particle.DustOptions white = new Particle.DustOptions(Color.WHITE, 2.5f);
        w.spawnParticle(Particle.FLAME, center, 200, 8, 8, 8, 0.2);
        w.spawnParticle(Particle.DUST, center, 500, 10, 10, 10, 0, purple);
        w.spawnParticle(Particle.DUST, center, 300, 7, 7, 7, 0, white);

        // Deal 60 damage to all entities within 15 blocks
        for (LivingEntity le : getEntitiesInRadius(center, 15, null)) {
            if (le instanceof Player lp && lp.getUniqueId().equals(caster.getUniqueId())) {
                double surviveDmg = Math.min(30.0, lp.getHealth() - 2.0);
                if (surviveDmg > 0) lp.damage(surviveDmg, caster);
            } else {
                le.damage(60.0, caster);
            }
        }

        // Destroy blocks in 15-block radius sphere
        Bukkit.getScheduler().runTaskLater(plugin, () ->
                destroyBlocksInRadius(center, 15, caster), 5L);

        caster.sendMessage(plugin.cfg().prefix() + "§5§lHOLLOW PURPLE NUKE §7detonated!");
    }

    // ===== DOMAIN EXPANSION: INFINITE VOID =====

    public void castInfiniteVoid(Player p) {
        if (!checkTechnique(p)) return;
        if (!checkCooldown(p, "domain_infinite_void", 300)) return;

        int cost = effectiveCost(p, 0.50);
        if (!plugin.ce().tryConsume(p.getUniqueId(), cost)) {
            p.sendMessage(plugin.cfg().prefix() + "§cNot enough Cursed Energy.");
            return;
        }

        plugin.cooldowns().setCooldown(p.getUniqueId(), "domain_infinite_void", 300);
        plugin.domainManager().expand(p, new InfiniteVoidDomain(plugin, p));
        p.sendMessage(plugin.cfg().prefix() + "§1§lInfinite Void §7— domain expanded!");
    }

    // ===== Utilities =====

    private List<LivingEntity> getEntitiesInRadius(Location center, double radius, Player exclude) {
        List<LivingEntity> result = new ArrayList<>();
        if (center.getWorld() == null) return result;
        for (Entity e : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (!(e instanceof LivingEntity le)) continue;
            if (exclude != null && e.getUniqueId().equals(exclude.getUniqueId())) continue;
            if (e.getLocation().distance(center) <= radius) result.add(le);
        }
        return result;
    }

    private void destroyBlocksInRadius(Location center, int radius, Player ignore) {
        World w = center.getWorld();
        if (w == null) return;
        int cx = center.getBlockX(), cy = center.getBlockY(), cz = center.getBlockZ();
        int r2 = radius * radius;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x * x + y * y + z * z > r2) continue;
                    Block b = w.getBlockAt(cx + x, cy + y, cz + z);
                    if (!b.getType().isAir() && b.getType() != Material.BEDROCK) {
                        b.setType(Material.AIR, false);
                    }
                }
            }
        }
    }

    /** Called when a player leaves to clean up their Infinity state and Blue orbs. */
    public void onPlayerQuit(UUID uuid) {
        BukkitTask t = infinityParticleTasks.remove(uuid);
        if (t != null) t.cancel();

        // Cleanup Blue orbs
        ItemDisplay blueOrb = activeBlueOrbs.remove(uuid);
        if (blueOrb != null && blueOrb.isValid()) blueOrb.remove();

        ItemDisplay blueMaxOrb = activeBlueMaxOrbs.remove(uuid);
        if (blueMaxOrb != null && blueMaxOrb.isValid()) blueMaxOrb.remove();

        BukkitTask blueTask = blueMaxActiveTasks.remove(uuid);
        if (blueTask != null) blueTask.cancel();

        ItemDisplay lockedOrb = lockedBlueOrbs.remove(uuid);
        if (lockedOrb != null && lockedOrb.isValid()) lockedOrb.remove();

        canLockBlue.remove(uuid);
    }

    /**
     * Returns the location of the player's locked Blue Max orb (for status display).
     * Returns null if there is no locked orb.
     */
    public Location getStationaryBlueLocation(UUID uuid) {
        ItemDisplay orb = lockedBlueOrbs.get(uuid);
        if (orb == null || !orb.isValid()) return null;
        return orb.getLocation();
    }
}
