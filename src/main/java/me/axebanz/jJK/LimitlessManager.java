package me.axebanz.jJK;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all Limitless Technique abilities:
 * Infinity, Blue, Blue Max, Red, Red Max, Hollow Purple, Hollow Purple Nuke.
 */
public final class LimitlessManager {

    private final JJKCursedToolsPlugin plugin;

    // Infinity state
    private final Map<UUID, BukkitTask> infinityParticleTasks = new ConcurrentHashMap<>();

    // Stationary Blue Max spheres: uuid -> location
    private final Map<UUID, Location> stationaryBlueSpheres = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> stationaryBlueTasks = new ConcurrentHashMap<>();

    // Red Max: players currently "holding" the red sphere
    private final Map<UUID, BukkitTask> redMaxHoldTasks = new ConcurrentHashMap<>();
    private final Map<UUID, Location> redMaxHeldLocations = new ConcurrentHashMap<>();

    // ===== CE costs (standard) — Six Eyes makes these 0 =====
    public static final int CE_COST_BLUE = 1;
    public static final int CE_COST_BLUE_MAX = 2;
    public static final int CE_COST_RED = 1;
    public static final int CE_COST_RED_MAX = 2;
    public static final int CE_COST_PURPLE = 3;
    public static final int CE_COST_NUKE = 5;

    public LimitlessManager(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    // ===== Helpers =====

    private boolean hasTechnique(Player p) {
        String id = plugin.techniqueManager().getAssignedId(p.getUniqueId());
        return "limitless".equalsIgnoreCase(id);
    }

    private boolean hasSixEyes(Player p) {
        return plugin.sixEyes() != null && plugin.sixEyes().hasSixEyes(p);
    }

    private int effectiveCost(Player p, int cost) {
        return hasSixEyes(p) ? 0 : cost;
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

    // ===== BLUE (Cursed Technique Lapse) =====

    public void castBlue(Player p) {
        if (!checkTechnique(p)) return;
        if (!checkCooldown(p, "limitless_blue", 8)) return;

        int cost = effectiveCost(p, CE_COST_BLUE);
        if (!plugin.ce().tryConsume(p.getUniqueId(), cost)) {
            p.sendMessage(plugin.cfg().prefix() + "§cNot enough Cursed Energy.");
            return;
        }

        plugin.cooldowns().setCooldown(p.getUniqueId(), "limitless_blue", 8);

        Location target = findTargetLocation(p, 50);
        spawnBlueSphere(p, target, 5, 8, 5, false);

        p.sendMessage(plugin.cfg().prefix() + "§bBlue §7— entities pulled!");
        p.playSound(p.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 0.8f, 1.8f);
    }

    private void spawnBlueSphere(Player caster, Location center, double radius, double damagePerSecond, int durationSeconds, boolean isMax) {
        World w = center.getWorld();
        if (w == null) return;

        int particleCount = isMax ? 20 : 8;
        double particleRadius = isMax ? 2.5 : 1.0;

        int[] ticksLeft = {durationSeconds * 20};
        Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(0, 80, 255), isMax ? 2.0f : 1.2f);

        BukkitTask[] taskRef = {null};
        taskRef[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            ticksLeft[0] -= 2;
            if (ticksLeft[0] <= 0) {
                taskRef[0].cancel();
                // Dissipate particles
                w.spawnParticle(Particle.EXPLOSION, center, 1, 0, 0, 0, 0);
                return;
            }

            // Spawn sphere particles
            spawnSphereParticles(w, center, particleRadius, particleCount, dust);

            // Pull entities every 10 ticks (0.5s)
            if (ticksLeft[0] % 10 == 0) {
                double dmg = damagePerSecond * 0.5; // per 0.5 seconds
                for (LivingEntity le : getEntitiesInRadius(center, radius, caster)) {
                    // Pull toward center
                    Vector pull = center.toVector().subtract(le.getLocation().toVector());
                    double dist = pull.length();
                    if (dist > 0.1) {
                        pull = pull.normalize().multiply(Math.min(1.5, dist * 0.3));
                        le.setVelocity(le.getVelocity().add(pull));
                    }
                    le.damage(dmg, caster);

                    if (isMax && le.isDead()) {
                        // Entity died from Max Blue
                        setCanNuke(caster, true);
                    }
                }
            }
        }, 0L, 2L);
    }

    // ===== BLUE MAX =====

    public void castBlueMax(Player p) {
        if (!checkTechnique(p)) return;
        if (!checkCooldown(p, "limitless_blue_max", 15)) return;

        int cost = effectiveCost(p, CE_COST_BLUE_MAX);
        if (!plugin.ce().tryConsume(p.getUniqueId(), cost)) {
            p.sendMessage(plugin.cfg().prefix() + "§cNot enough Cursed Energy.");
            return;
        }

        plugin.cooldowns().setCooldown(p.getUniqueId(), "limitless_blue_max", 15);

        UUID uuid = p.getUniqueId();

        p.sendMessage(plugin.cfg().prefix() + "§bBlue Maximum Output §7— controlling sphere. §eShift to release.");
        p.playSound(p.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 0.8f, 2.0f);

        int[] ticks = {0};
        final int MAX_DURATION = 200; // 10 seconds of control
        Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(0, 100, 255), 2.5f);

        BukkitTask[] taskRef = {null};
        taskRef[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!p.isOnline()) { taskRef[0].cancel(); return; }

            ticks[0]++;

            // Follow player's look direction
            Location sphereLoc = p.getEyeLocation().add(p.getLocation().getDirection().multiply(5));

            // Sphere particles
            spawnSphereParticles(p.getWorld(), sphereLoc, 2.5, 20, dust);

            // Destroy blocks in path
            if (ticks[0] % 5 == 0) {
                destroyBlocksInRadius(sphereLoc, 2, p);
            }

            // Pull entities
            if (ticks[0] % 10 == 0) {
                boolean killedSomething = false;
                for (LivingEntity le : getEntitiesInRadius(sphereLoc, 15, p)) {
                    Vector pull = sphereLoc.toVector().subtract(le.getLocation().toVector());
                    double dist = pull.length();
                    if (dist > 0.1) pull = pull.normalize().multiply(Math.min(2.0, dist * 0.4));
                    le.setVelocity(le.getVelocity().add(pull));
                    le.damage(7.5, p); // 15/s in 0.5s intervals
                    if (le.isDead()) killedSomething = true;
                }
                if (killedSomething) {
                    setCanNuke(p, true);
                    p.sendMessage("§b§lYou have the ability to release a nuke");
                }
            }

            // Player pressed shift OR max duration
            if (p.isSneaking() || ticks[0] >= MAX_DURATION) {
                taskRef[0].cancel();
                // Sphere stays for 90 seconds
                Location finalLoc = p.getEyeLocation().add(p.getLocation().getDirection().multiply(5));
                makeStationaryBlue(p, finalLoc);
                p.sendMessage(plugin.cfg().prefix() + "§bBlue sphere §7anchored for 90 seconds.");
            }
        }, 0L, 2L);
    }

    private void makeStationaryBlue(Player caster, Location loc) {
        UUID uuid = caster.getUniqueId();

        // Cancel existing
        BukkitTask old = stationaryBlueTasks.remove(uuid);
        if (old != null) old.cancel();

        stationaryBlueSpheres.put(uuid, loc.clone());
        Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(0, 100, 255), 2.5f);

        int[] ticksLeft = {90 * 20}; // 90 seconds
        BukkitTask[] taskRef = {null};
        taskRef[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            ticksLeft[0] -= 2;
            if (ticksLeft[0] <= 0 || !stationaryBlueSpheres.containsKey(uuid)) {
                taskRef[0].cancel();
                stationaryBlueSpheres.remove(uuid);
                return;
            }

            Location center = stationaryBlueSpheres.get(uuid);
            spawnSphereParticles(center.getWorld(), center, 2.5, 20, dust);

            if (ticksLeft[0] % 10 == 0) {
                for (LivingEntity le : getEntitiesInRadius(center, 15, caster)) {
                    Vector pull = center.toVector().subtract(le.getLocation().toVector());
                    double dist = pull.length();
                    if (dist > 0.1) pull = pull.normalize().multiply(Math.min(2.0, dist * 0.4));
                    le.setVelocity(le.getVelocity().add(pull));
                    le.damage(7.5, caster);
                }
            }
        }, 0L, 2L);
        stationaryBlueTasks.put(uuid, taskRef[0]);
    }

    private void setCanNuke(Player p, boolean val) {
        PlayerProfile prof = plugin.data().get(p.getUniqueId());
        prof.limitlessCanNuke = val;
        plugin.data().save(p.getUniqueId());
    }

    // ===== RED (Cursed Technique Reversal) =====

    public void castRed(Player p) {
        if (!checkTechnique(p)) return;

        // Requires RCT (200 CE for Limitless)
        if (!plugin.ce().hasRct(p.getUniqueId())) {
            p.sendMessage(plugin.cfg().prefix() + "§cRed requires §aReverse Cursed Technique §cunlocked! §7(Need 200 CE level)");
            return;
        }
        if (!checkCooldown(p, "limitless_red", 8)) return;

        int cost = effectiveCost(p, CE_COST_RED);
        if (!plugin.ce().tryConsume(p.getUniqueId(), cost)) {
            p.sendMessage(plugin.cfg().prefix() + "§cNot enough Cursed Energy.");
            return;
        }

        plugin.cooldowns().setCooldown(p.getUniqueId(), "limitless_red", 8);

        Location target = findTargetLocation(p, 50);
        spawnRedSphere(p, target, 5, 20, 3, false);

        p.sendMessage(plugin.cfg().prefix() + "§cRed §7— entities repelled!");
        p.playSound(p.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 0.8f, 0.7f);
    }

    private void spawnRedSphere(Player caster, Location center, double radius, double impactDamage, int durationSeconds, boolean isMax) {
        World w = center.getWorld();
        if (w == null) return;

        double particleRadius = isMax ? 2.0 : 1.0;
        int particleCount = isMax ? 18 : 8;
        Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(220, 0, 0), isMax ? 2.0f : 1.2f);

        // Immediate repulsion + damage
        for (LivingEntity le : getEntitiesInRadius(center, radius, caster)) {
            Vector push = le.getLocation().toVector().subtract(center.toVector());
            double dist = push.length();
            if (dist < 0.1) push = new Vector(1, 0.5, 0);
            else push = push.normalize().multiply(isMax ? 4.0 : 2.5).add(new Vector(0, 0.5, 0));
            le.setVelocity(push);
            le.damage(impactDamage, caster);
        }

        // Short visual effect
        int[] ticks = {durationSeconds * 10};
        BukkitTask[] taskRef = {null};
        taskRef[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            ticks[0] -= 2;
            if (ticks[0] <= 0) { taskRef[0].cancel(); return; }
            spawnSphereParticles(w, center, particleRadius, particleCount, dust);
        }, 0L, 2L);

        w.spawnParticle(Particle.EXPLOSION, center, 3, 0.5, 0.5, 0.5, 0);
        w.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1.4f);
    }

    // ===== RED MAX =====

    public void castRedMax(Player p) {
        if (!checkTechnique(p)) return;

        if (!plugin.ce().hasRct(p.getUniqueId())) {
            p.sendMessage(plugin.cfg().prefix() + "§cRed Max requires §aReverse Cursed Technique§c!");
            return;
        }
        if (!checkCooldown(p, "limitless_red_max", 12)) return;

        int cost = effectiveCost(p, CE_COST_RED_MAX);
        if (!plugin.ce().tryConsume(p.getUniqueId(), cost)) {
            p.sendMessage(plugin.cfg().prefix() + "§cNot enough Cursed Energy.");
            return;
        }

        plugin.cooldowns().setCooldown(p.getUniqueId(), "limitless_red_max", 12);

        UUID uuid = p.getUniqueId();
        p.sendMessage(plugin.cfg().prefix() + "§cRed Maximum Output §7— aim and §eShift to launch!");
        p.playSound(p.getLocation(), Sound.BLOCK_PORTAL_AMBIENT, 0.8f, 0.5f);

        Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(220, 0, 0), 2.0f);
        int[] ticks = {0};

        BukkitTask[] taskRef = {null};
        taskRef[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!p.isOnline()) { taskRef[0].cancel(); return; }
            ticks[0]++;

            // Sphere in player's hand position
            Location holdLoc = p.getEyeLocation().add(p.getLocation().getDirection().multiply(1.5));
            spawnSphereParticles(p.getWorld(), holdLoc, 1.5, 12, dust);

            // Launch on shift or after 5 seconds
            if (p.isSneaking() || ticks[0] >= 100) {
                taskRef[0].cancel();
                launchRedMax(p);
            }
        }, 0L, 2L);
        redMaxHoldTasks.put(uuid, taskRef[0]);
    }

    private void launchRedMax(Player caster) {
        UUID uuid = caster.getUniqueId();
        redMaxHoldTasks.remove(uuid);

        Location start = caster.getEyeLocation();
        Vector dir = caster.getLocation().getDirection().normalize();
        Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(220, 0, 0), 2.0f);

        int[] ticks = {0};
        BukkitTask[] taskRef = {null};

        // Check if there's a stationary blue sphere — if so, trigger nuke
        Location bluePos = stationaryBlueSpheres.get(uuid);

        taskRef[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            ticks[0]++;
            if (ticks[0] > 80) { taskRef[0].cancel(); return; } // max 4s travel

            Location pos = start.clone().add(dir.clone().multiply(ticks[0] * 0.5));
            spawnSphereParticles(pos.getWorld(), pos, 1.5, 10, dust);

            // Nuke check
            if (bluePos != null && pos.distance(bluePos) < 4.0) {
                taskRef[0].cancel();
                triggerNuke(caster, bluePos);
                return;
            }

            // Hit entities
            for (LivingEntity le : getEntitiesInRadius(pos, 3, caster)) {
                Vector push = le.getLocation().toVector().subtract(pos.toVector());
                if (push.length() < 0.1) push = new Vector(1, 0.5, 0);
                else push = push.normalize().multiply(5.0).add(new Vector(0, 1, 0));
                le.setVelocity(push);
                le.damage(25, caster);
                taskRef[0].cancel();
            }
        }, 0L, 1L);
    }

    // ===== HOLLOW PURPLE =====

    public void castHollowPurple(Player p) {
        if (!checkTechnique(p)) return;

        if (!plugin.ce().hasRct(p.getUniqueId())) {
            p.sendMessage(plugin.cfg().prefix() + "§cHollow Purple requires §aReverse Cursed Technique§c!");
            return;
        }
        if (!checkCooldown(p, "limitless_purple", 20)) return;

        int cost = effectiveCost(p, CE_COST_PURPLE);
        if (!plugin.ce().tryConsume(p.getUniqueId(), cost)) {
            p.sendMessage(plugin.cfg().prefix() + "§cNot enough Cursed Energy.");
            return;
        }

        plugin.cooldowns().setCooldown(p.getUniqueId(), "limitless_purple", 20);

        p.sendMessage(plugin.cfg().prefix() + "§5Hollow Purple §7— fired!");
        p.playSound(p.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 0.5f);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 0.8f, 1.2f);

        Location start = p.getEyeLocation();
        Vector dir = p.getLocation().getDirection().normalize();
        Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(160, 0, 200), 2.5f);
        Particle.DustOptions white = new Particle.DustOptions(Color.WHITE, 1.5f);

        int[] ticks = {0};
        Set<UUID> hit = new HashSet<>();
        BukkitTask[] taskRef = {null};

        taskRef[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            ticks[0]++;
            if (ticks[0] > 200) { taskRef[0].cancel(); return; } // max 10s travel

            Location pos = start.clone().add(dir.clone().multiply(ticks[0] * 0.75));

            // Particles
            spawnSphereParticles(pos.getWorld(), pos, 1.5, 12, dust);
            pos.getWorld().spawnParticle(Particle.DUST, pos, 3, 0.5, 0.5, 0.5, 0, white);

            // Destroy blocks
            if (ticks[0] % 3 == 0) destroyBlocksInRadius(pos, 2, p);

            // Damage entities in radius
            for (LivingEntity le : getEntitiesInRadius(pos, 3, p)) {
                if (!hit.contains(le.getUniqueId())) {
                    hit.add(le.getUniqueId());
                    le.damage(50, p);
                }
            }

            // Stop if hits a solid block
            if (pos.getBlock().getType().isSolid()) {
                taskRef[0].cancel();
                pos.getWorld().spawnParticle(Particle.EXPLOSION, pos, 5, 1, 1, 1, 0);
            }
        }, 0L, 1L);
    }

    // ===== HOLLOW PURPLE NUKE =====

    public void castNuke(Player p) {
        if (!checkTechnique(p)) return;

        PlayerProfile prof = plugin.data().get(p.getUniqueId());
        if (!prof.limitlessCanNuke) {
            p.sendMessage(plugin.cfg().prefix() + "§cNuke not available. Kill something with §bMax Blue §cfirst.");
            return;
        }

        Location bluePos = stationaryBlueSpheres.get(p.getUniqueId());
        if (bluePos == null) {
            p.sendMessage(plugin.cfg().prefix() + "§cNo stationary Blue sphere. Use §bBlue Max§c and anchor it first.");
            return;
        }

        int cost = effectiveCost(p, CE_COST_NUKE);
        if (!plugin.ce().tryConsume(p.getUniqueId(), cost)) {
            p.sendMessage(plugin.cfg().prefix() + "§cNot enough Cursed Energy.");
            return;
        }

        // Launch red max, it will auto-trigger nuke when it hits blue
        setCanNuke(p, false);
        launchRedMax(p);

        p.sendMessage(plugin.cfg().prefix() + "§5§lHOLLOW PURPLE NUKE §7— incoming!");
    }

    private void triggerNuke(Player caster, Location center) {
        // Clean up stationary sphere
        UUID uuid = caster.getUniqueId();
        BukkitTask task = stationaryBlueTasks.remove(uuid);
        if (task != null) task.cancel();
        stationaryBlueSpheres.remove(uuid);

        World w = center.getWorld();
        if (w == null) return;

        // Screen shake simulation
        caster.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 30, 0, false, false, false));
        w.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 4.0f, 0.3f);
        w.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 3.0f, 0.3f);

        // Massive explosion — 50+ block radius
        int nukeRadius = 55;
        Particle.DustOptions purple = new Particle.DustOptions(Color.fromRGB(160, 0, 200), 3.0f);
        Particle.DustOptions white = new Particle.DustOptions(Color.WHITE, 2.5f);

        // Massive particle burst
        w.spawnParticle(Particle.EXPLOSION, center, 20, nukeRadius / 4.0, nukeRadius / 4.0, nukeRadius / 4.0, 0);
        w.spawnParticle(Particle.DUST, center, 500, nukeRadius / 2.0, nukeRadius / 2.0, nukeRadius / 2.0, 0, purple);
        w.spawnParticle(Particle.DUST, center, 300, nukeRadius / 3.0, nukeRadius / 3.0, nukeRadius / 3.0, 0, white);

        // Damage all entities in range (100+ damage)
        for (LivingEntity le : getEntitiesInRadius(center, nukeRadius, null)) {
            if (le instanceof Player lp && lp.getUniqueId().equals(caster.getUniqueId())) {
                // User takes ~30 damage but won't die (set to 1 HP min)
                double surviveDmg = Math.min(30.0, lp.getHealth() - 2.0);
                if (surviveDmg > 0) lp.damage(surviveDmg, caster);
            } else {
                le.damage(120, caster);
            }
        }

        // Destroy blocks in a huge radius (do this in stages to avoid lag)
        Bukkit.getScheduler().runTaskLater(plugin, () ->
                destroyBlocksInRadius(center, nukeRadius / 2, caster), 5L);

        // Notify caster
        caster.sendMessage(plugin.cfg().prefix() + "§5§lHOLLOW PURPLE NUKE §7detonated!");
    }

    // ===== DOMAIN EXPANSION: INFINITE VOID =====

    public void castInfiniteVoid(Player p) {
        if (!checkTechnique(p)) return;
        if (!checkCooldown(p, "domain_infinite_void", 300)) return;

        int cost = effectiveCost(p, 5);
        if (!plugin.ce().tryConsume(p.getUniqueId(), cost)) {
            p.sendMessage(plugin.cfg().prefix() + "§cNot enough Cursed Energy.");
            return;
        }

        plugin.cooldowns().setCooldown(p.getUniqueId(), "domain_infinite_void", 300);
        plugin.domainManager().expand(p, new InfiniteVoidDomain(plugin, p));
        p.sendMessage(plugin.cfg().prefix() + "§1§lInfinite Void §7— domain expanded!");
    }

    // ===== Utilities =====

    private Location findTargetLocation(Player p, int maxBlocks) {
        Location eye = p.getEyeLocation();
        Vector dir = p.getLocation().getDirection().normalize();
        for (int i = 1; i <= maxBlocks; i++) {
            Location loc = eye.clone().add(dir.clone().multiply(i));
            if (loc.getBlock().getType().isSolid()) {
                return loc.clone().subtract(dir);
            }
        }
        return eye.clone().add(dir.clone().multiply(maxBlocks));
    }

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

    private void spawnSphereParticles(World w, Location center, double radius, int count, Particle.DustOptions dust) {
        if (w == null) return;
        double step = Math.PI * 2.0 / count;
        for (int i = 0; i < count; i++) {
            double theta = step * i;
            double phi = (System.currentTimeMillis() % 6000) / 6000.0 * Math.PI;
            double x = radius * Math.sin(phi) * Math.cos(theta);
            double y = radius * Math.cos(phi);
            double z = radius * Math.sin(phi) * Math.sin(theta);
            w.spawnParticle(Particle.DUST, center.clone().add(x, y, z), 1, 0, 0, 0, 0, dust);
        }
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

    /** Called when a player leaves to clean up their Infinity state. */
    public void onPlayerQuit(UUID uuid) {
        BukkitTask t = infinityParticleTasks.remove(uuid);
        if (t != null) t.cancel();
        BukkitTask b = stationaryBlueTasks.remove(uuid);
        if (b != null) b.cancel();
        stationaryBlueSpheres.remove(uuid);
        BukkitTask r = redMaxHoldTasks.remove(uuid);
        if (r != null) r.cancel();
    }

    public Location getStationaryBlueLocation(UUID uuid) {
        return stationaryBlueSpheres.get(uuid);
    }
}
