package me.axebanz.jJK;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class ProjectionManager {

    private static final int PENALTY_TICKS = 20;

    // Base distance 8 blocks, +1 per stack
    private static final double BASE_DISTANCE = 8.0;
    private static final double DISTANCE_PER_STACK = 1.0;
    private static final int MAX_STACKS = 3;
    private static final long OVERHEAT_CD_SECONDS = 5;
    private static final int CE_COST = 2;
    private static final int BREAKER_CE_COST = 3;
    private static final long USE_CD_SECONDS = 5;
    private static final long BREAKER_CD_SECONDS = 10;
    private static final double LOCK_RANGE = 20.0;
    private static final double LOCK_CONE_COS = Math.cos(Math.toRadians(45));
    private static final double FREEZE_PATH_RADIUS = 1.5;

    private final JJKCursedToolsPlugin plugin;
    private final ProjectionVisuals visuals;
    private final ProjectionFreezeHandler freezeHandler;

    private final Map<UUID, ProjectionPlayerData> dataMap = new ConcurrentHashMap<>();

    private int taskId = -1;

    public ProjectionManager(JJKCursedToolsPlugin plugin, ProjectionVisuals visuals, ProjectionFreezeHandler freezeHandler) {
        this.plugin = plugin;
        this.visuals = visuals;
        this.freezeHandler = freezeHandler;
    }

    public void start() {
        if (taskId != -1) Bukkit.getScheduler().cancelTask(taskId);
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::tick, 1L, 1L);
    }

    public void stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    public ProjectionPlayerData getData(UUID uuid) {
        return dataMap.get(uuid);
    }

    private ProjectionPlayerData getOrCreate(UUID uuid) {
        return dataMap.computeIfAbsent(uuid, ProjectionPlayerData::new);
    }

    public void autoLockOnHit(Player attacker, LivingEntity target) {
        UUID u = attacker.getUniqueId();
        ProjectionPlayerData data = getOrCreate(u);

        if (data.lockedTarget != null && data.lockedTarget.equals(target.getUniqueId())) return;

        cancelLockTask(data);
        data.lockedTarget = target.getUniqueId();

        String tName = target instanceof Player ? ((Player) target).getName() : target.getType().name();
        attacker.sendMessage(plugin.cfg().prefix() + "§9Locked onto: §f" + tName);

        final UUID targetId = target.getUniqueId();
        int lockTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            Player pp = Bukkit.getPlayer(u);
            if (pp == null) return;
            Entity t = Bukkit.getEntity(targetId);
            if (t == null) {
                data.lockedTarget = null;
                cancelLockTask(data);
                return;
            }
            if (t.getLocation().distanceSquared(pp.getLocation()) > 30 * 30) {
                data.lockedTarget = null;
                cancelLockTask(data);
                pp.sendMessage(plugin.cfg().prefix() + "§cLock lost: target too far.");
                return;
            }
            visuals.spawnLockBeam(pp, t.getLocation().clone().add(0, 1, 0));
        }, 0L, 2L);
        data.lockParticleTask = lockTaskId;
    }

    public void tryActivate(Player p) {
        UUID u = p.getUniqueId();
        ProjectionPlayerData data = getOrCreate(u);

        if (!plugin.techniqueManager().canUseTechniqueActions(p, true)) return;

        if (data.state != ProjectionState.IDLE) {
            p.sendMessage(plugin.cfg().prefix() + "§cProjection is already active.");
            return;
        }

        if (plugin.cooldowns().isOnCooldown(u, "projection.dash")) {
            long rem = plugin.cooldowns().remainingSeconds(u, "projection.dash");
            p.sendMessage(plugin.cfg().prefix() + "§cProjection on cooldown: §f" + TimeFmt.mmss(rem));
            return;
        }

        if (!plugin.ce().tryConsume(u, CE_COST)) {
            p.sendMessage(plugin.cfg().prefix() + "§cNot enough Cursed Energy. §7(Need " + CE_COST + ")");
            return;
        }

        if (data.lockedTarget == null) {
            autoLockOnLook(p, data);
        }

        double totalDistance = BASE_DISTANCE + (data.stacks * DISTANCE_PER_STACK);

        Vector dir;
        if (data.lockedTarget != null) {
            Entity target = Bukkit.getEntity(data.lockedTarget);
            if (target != null && target.getLocation().distanceSquared(p.getLocation()) <= LOCK_RANGE * LOCK_RANGE) {
                dir = target.getLocation().clone().subtract(p.getLocation()).toVector();
                dir.setY(0);
                if (dir.lengthSquared() > 0.01) {
                    dir.normalize();
                } else {
                    dir = flatLookDir(p);
                }
            } else {
                dir = flatLookDir(p);
            }
        } else {
            dir = flatLookDir(p);
        }

        // Pass through: freeze entities along the ENTIRE path
        Location startLoc = p.getLocation().clone();
        for (double d = 0; d <= totalDistance; d += 0.5) {
            Location point = startLoc.clone().add(dir.clone().multiply(d));
            for (Entity e : p.getWorld().getNearbyEntities(point, FREEZE_PATH_RADIUS, FREEZE_PATH_RADIUS, FREEZE_PATH_RADIUS)) {
                if (e.equals(p)) continue;
                if (!(e instanceof LivingEntity le)) continue;
                if (e instanceof org.bukkit.entity.ArmorStand) continue;
                if (plugin.rika() != null && plugin.rika().isRikaEntity(e)) continue;

                if (le instanceof Player victim) {
                    freezeHandler.tryPathFreeze(p, victim);
                } else {
                    freezeHandler.tryPathFreezeEntity(p, le);
                }
            }
        }

        // Destroy blocks in path
        for (double d = 0; d <= totalDistance; d += 1.0) {
            Location point = startLoc.clone().add(dir.clone().multiply(d));
            destroyBlocksAt(point);
        }

        // Teleport to END of line (pass through everything)
        Location destination = startLoc.clone().add(dir.clone().multiply(totalDistance));
        Location snapped = snapToGround(destination);
        snapped.setYaw(p.getLocation().getYaw());
        snapped.setPitch(p.getLocation().getPitch());

        visuals.spawnTrail(p);
        visuals.spawnAfterimage(p);

        p.teleport(snapped);

        visuals.spawnTrail(p);
        visuals.playStepSound(p);

        applyPostDashSpeed(p, data.stacks);

        int newStacks = data.stacks + 1;
        if (newStacks > MAX_STACKS) {
            data.stacks = 0;
            data.reset();
            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 5 * 20, 0, false, false, true));
            plugin.cooldowns().setCooldown(u, "projection.dash", OVERHEAT_CD_SECONDS);
            plugin.actionbarUI().setTimer(u, "projection.dash", "■", "§9", OVERHEAT_CD_SECONDS);
            visuals.showOverheat(p);
        } else {
            data.stacks = newStacks;
            plugin.cooldowns().setCooldown(u, "projection.dash", USE_CD_SECONDS);
            plugin.actionbarUI().setTimer(u, "projection.dash", "■", "§9", USE_CD_SECONDS);
            p.sendActionBar("§9Projection complete. §9Stack: " + data.stacks + " §7(Range: " + (int)(BASE_DISTANCE + data.stacks * DISTANCE_PER_STACK) + "b)");
        }
    }

    private Vector flatLookDir(Player p) {
        Vector dir = p.getLocation().getDirection().normalize();
        dir.setY(0);
        if (dir.lengthSquared() < 0.01) dir = new Vector(1, 0, 0);
        return dir.normalize();
    }

    private void autoLockOnLook(Player p, ProjectionPlayerData data) {
        Vector lookDir = p.getLocation().getDirection().normalize();
        Location playerLoc = p.getLocation().clone().add(0, 1, 0);

        Entity best = null;
        double bestDist = Double.MAX_VALUE;

        for (Entity e : p.getWorld().getNearbyEntities(p.getLocation(), LOCK_RANGE, LOCK_RANGE, LOCK_RANGE)) {
            if (e.equals(p)) continue;
            if (!(e instanceof LivingEntity)) continue;
            if (e instanceof org.bukkit.entity.ArmorStand) continue;

            Vector toEntity = e.getLocation().clone().add(0, 1, 0).subtract(playerLoc).toVector().normalize();
            double dot = lookDir.dot(toEntity);

            if (dot >= LOCK_CONE_COS) {
                double dist = e.getLocation().distanceSquared(p.getLocation());
                if (dist < bestDist) {
                    bestDist = dist;
                    best = e;
                }
            }
        }

        if (best != null) {
            data.lockedTarget = best.getUniqueId();
            String name = best instanceof Player ? ((Player) best).getName() : best.getType().name();
            p.sendMessage(plugin.cfg().prefix() + "§9Auto-locked onto: §f" + name);
        }
    }

    public void tryBreaker(Player p) {
        UUID u = p.getUniqueId();
        ProjectionPlayerData data = getOrCreate(u);

        if (!plugin.techniqueManager().canUseTechniqueActions(p, true)) return;

        if (data.frozenTarget == null) {
            p.sendMessage(plugin.cfg().prefix() + "§cNo frozen target! Freeze someone with Projection Dash first.");
            return;
        }

        Entity target = Bukkit.getEntity(data.frozenTarget);
        if (target == null || !freezeHandler.isFrozen(data.frozenTarget)) {
            data.frozenTarget = null;
            p.sendMessage(plugin.cfg().prefix() + "§cYour frozen target is no longer frozen.");
            return;
        }

        if (plugin.cooldowns().isOnCooldown(u, "projection.breaker")) {
            long rem = plugin.cooldowns().remainingSeconds(u, "projection.breaker");
            p.sendMessage(plugin.cfg().prefix() + "§cBreaker on cooldown: §f" + TimeFmt.mmss(rem));
            return;
        }

        if (!plugin.ce().tryConsume(u, BREAKER_CE_COST)) {
            p.sendMessage(plugin.cfg().prefix() + "§cNot enough Cursed Energy. §7(Need " + BREAKER_CE_COST + ")");
            return;
        }

        Vector dir = p.getLocation().getDirection().normalize();
        dir.setY(0);
        if (dir.lengthSquared() < 0.01) dir = new Vector(1, 0, 0);
        dir.normalize();

        Location backLoc = snapToGround(p.getLocation().clone().add(dir.clone().multiply(-5)));
        backLoc.setYaw(p.getLocation().getYaw());
        backLoc.setPitch(p.getLocation().getPitch());
        p.teleport(backLoc);
        visuals.spawnTrail(p);

        p.sendActionBar("§9◀ PROJECTION BREAKER §8| §fLunging...");

        final UUID frozenId = data.frozenTarget;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Entity frozenEnt = Bukkit.getEntity(frozenId);
            if (frozenEnt == null) {
                data.frozenTarget = null;
                data.reset();
                return;
            }

            Location targetLoc = frozenEnt.getLocation().clone();
            targetLoc.setYaw(p.getLocation().getYaw());
            targetLoc.setPitch(p.getLocation().getPitch());
            p.teleport(targetLoc);
            visuals.spawnTrail(p);

            freezeHandler.breakerShatter(p, frozenId);
            data.frozenTarget = null;
            data.reset();
            plugin.cooldowns().setCooldown(p.getUniqueId(), "projection.breaker", BREAKER_CD_SECONDS);
            visuals.spawnBreakerExplosion(p);
            p.sendActionBar("§9◀ PROJECTION BREAKER §8| §aSHATTERED!");
        }, 5L);
    }

    public void tryCancel(Player p) {
        UUID u = p.getUniqueId();
        ProjectionPlayerData data = dataMap.get(u);

        if (data == null || data.state == ProjectionState.IDLE) {
            p.sendMessage(plugin.cfg().prefix() + "§7Projection is not active.");
            return;
        }

        data.stacks = 0;
        cancelLockTask(data);
        data.reset();
        p.sendMessage(plugin.cfg().prefix() + "§9Projection cancelled. §7Stacks reset.");
    }

    public void tryLock(Player p) {
        UUID u = p.getUniqueId();
        ProjectionPlayerData data = getOrCreate(u);

        List<Entity> candidates = new ArrayList<>();
        for (Entity e : p.getWorld().getNearbyEntities(p.getLocation(), LOCK_RANGE, LOCK_RANGE, LOCK_RANGE)) {
            if (e.equals(p)) continue;
            if (!(e instanceof LivingEntity)) continue;
            if (e instanceof org.bukkit.entity.ArmorStand) continue;
            candidates.add(e);
        }

        if (candidates.isEmpty()) {
            p.sendMessage(plugin.cfg().prefix() + "§cNo targets within " + (int)LOCK_RANGE + " blocks.");
            return;
        }

        Entity current = data.lockedTarget != null ? Bukkit.getEntity(data.lockedTarget) : null;
        int currentIndex = -1;
        for (int i = 0; i < candidates.size(); i++) {
            if (candidates.get(i).equals(current)) {
                currentIndex = i;
                break;
            }
        }
        int nextIndex = (currentIndex + 1) % candidates.size();
        Entity nextTarget = candidates.get(nextIndex);

        cancelLockTask(data);
        data.lockedTarget = nextTarget.getUniqueId();

        String tName = nextTarget instanceof Player ? ((Player) nextTarget).getName() : nextTarget.getType().name();
        p.sendMessage(plugin.cfg().prefix() + "§9Locked onto: §f" + tName);

        final UUID targetId = nextTarget.getUniqueId();
        int lockTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            Player pp = Bukkit.getPlayer(u);
            if (pp == null) return;
            Entity t = Bukkit.getEntity(targetId);
            if (t == null) {
                data.lockedTarget = null;
                cancelLockTask(data);
                return;
            }
            if (t.getLocation().distanceSquared(pp.getLocation()) > 30 * 30) {
                data.lockedTarget = null;
                cancelLockTask(data);
                pp.sendMessage(plugin.cfg().prefix() + "§cLock lost: target too far.");
                return;
            }
            visuals.spawnLockBeam(pp, t.getLocation().clone().add(0, 1, 0));
        }, 0L, 2L);
        data.lockParticleTask = lockTaskId;
    }

    public void tryUnlock(Player p) {
        UUID u = p.getUniqueId();
        ProjectionPlayerData data = dataMap.get(u);
        if (data == null || data.lockedTarget == null) {
            p.sendMessage(plugin.cfg().prefix() + "§7No target locked.");
            return;
        }
        cancelLockTask(data);
        data.lockedTarget = null;
        p.sendMessage(plugin.cfg().prefix() + "§9Lock released.");
    }

    private void cancelLockTask(ProjectionPlayerData data) {
        if (data.lockParticleTask != -1) {
            Bukkit.getScheduler().cancelTask(data.lockParticleTask);
            data.lockParticleTask = -1;
        }
    }

    private void tick() {
        for (Map.Entry<UUID, ProjectionPlayerData> entry : dataMap.entrySet()) {
            UUID u = entry.getKey();
            ProjectionPlayerData data = entry.getValue();

            Player p = Bukkit.getPlayer(u);
            if (p == null || !p.isOnline()) {
                data.reset();
                continue;
            }

            if (data.state == ProjectionState.FROZEN_PENALTY) {
                tickPenalty(p, data);
            }
        }
    }

    private void tickPenalty(Player p, ProjectionPlayerData data) {
        data.phaseTicks--;
        visuals.updatePenaltyActionbar(p, data.phaseTicks);

        if (data.phaseTicks <= 0) {
            p.removePotionEffect(PotionEffectType.SLOWNESS);
            p.removePotionEffect(PotionEffectType.JUMP_BOOST);
            data.state = ProjectionState.IDLE;
            p.sendActionBar("§9Projection ready.");
        }
    }

    private void destroyBlocksAt(Location loc) {
        World w = loc.getWorld();
        if (w == null) return;
        Block foot = w.getBlockAt(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        Block head = w.getBlockAt(loc.getBlockX(), loc.getBlockY() + 1, loc.getBlockZ());
        if (!foot.getType().isAir() && !foot.isLiquid() && foot.getType().isSolid()) foot.breakNaturally();
        if (!head.getType().isAir() && !head.isLiquid() && head.getType().isSolid()) head.breakNaturally();
    }

    private void applyPostDashSpeed(Player p, int stacks) {
        if (stacks == 0) return;
        int level = switch (stacks) {
            case 1 -> 0;
            case 2 -> 1;
            default -> 2;
        };
        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 5 * 20, level, false, false, true));
    }

    public void onTakeDamage(Player p) {
        ProjectionPlayerData data = dataMap.get(p.getUniqueId());
        if (data == null) return;
        data.stacks = 0;
    }

    public void cleanup(UUID uuid) {
        ProjectionPlayerData data = dataMap.remove(uuid);
        if (data != null) cancelLockTask(data);
        freezeHandler.cleanupPlayer(uuid);
    }

    public void setFrozenTarget(UUID attacker, UUID target) {
        ProjectionPlayerData data = getOrCreate(attacker);
        data.frozenTarget = target;
    }

    private Location snapToGround(Location loc) {
        World w = loc.getWorld();
        if (w == null) return loc;
        int x = loc.getBlockX();
        int z = loc.getBlockZ();
        int startY = loc.getBlockY();
        for (int y = startY + 3; y >= Math.max(w.getMinHeight(), startY - 5); y--) {
            Block block = w.getBlockAt(x, y, z);
            Block above = w.getBlockAt(x, y + 1, z);
            if (!block.getType().isAir() && !block.isLiquid() && above.getType().isAir()) {
                return new Location(w, loc.getX(), y + 1.0, loc.getZ(), loc.getYaw(), loc.getPitch());
            }
        }
        return loc;
    }

    public ProjectionFreezeHandler getFreezeHandler() {
        return freezeHandler;
    }
}