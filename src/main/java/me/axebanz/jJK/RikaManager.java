package me.axebanz.jJK;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RikaManager {

    private static final String MODEL_ID = "rika";

    private static final String ANIM_SUMMON = "summon";
    private static final String ANIM_IDLE   = "idle";
    private static final String ANIM_ATTACK = "attack";
    private static final String ANIM_BEAM   = "pure love beam";

    private static final long ACTIVE_SECONDS = 5 * 60;
    private static final long SUMMON_CD_SECONDS = 5 * 60;
    private static final long BEAM_CD_SECONDS = 5 * 60;

    private static final double FOLLOW_DISTANCE = 3.0;
    private static final double HARD_LIMIT = 20.0;
    private static final double ATTACK_RANGE = 3.2;
    private static final long TARGET_TIMEOUT_MS = 9000L;

    private static final double FOLLOW_SPEED = 0.35;
    private static final double CHASE_SPEED = 0.55;
    private static final double Y_LERP_SPEED = 0.25;

    private static final double ATTACK_STOP_DISTANCE = 2.5;

    private static final long RESPAWN_GRACE_MS = 5000L;

    private static final int BEAM_COST = 50;
    private static final double BEAM_RANGE = 80.0;

    private static final double BEAM_SPEED = 3.0;
    private static final double BEAM_HIT_RADIUS = 3.5;
    private static final double BEAM_DAMAGE = 180.0;

    private static final int TRENCH_RADIUS = 6;

    private static final double HEAL_THRESHOLD_HP = 14.0;
    private static final long HEAL_CD_SECONDS = 30;

    private final JJKCursedToolsPlugin plugin;
    private final ModelEngineBridge model;

    private final NamespacedKey KEY_OWNER;
    private final NamespacedKey KEY_RIKA;
    private final NamespacedKey KEY_BEAM;

    private final Map<UUID, UUID> rikaByOwner = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> lastTargetByOwner = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastTargetAtMs = new ConcurrentHashMap<>();
    private final Map<UUID, Long> respawnGrace = new ConcurrentHashMap<>();

    private int brainTaskId = -1;

    public RikaManager(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
        this.model = new ModelEngineBridge(plugin);
        this.KEY_OWNER = new NamespacedKey(plugin, "rika_owner");
        this.KEY_RIKA = new NamespacedKey(plugin, "rika");
        this.KEY_BEAM = new NamespacedKey(plugin, "rika_beam");
    }

    public void start() {
        if (brainTaskId != -1) Bukkit.getScheduler().cancelTask(brainTaskId);
        brainTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::tick, 1L, 1L);
    }

    public boolean isActive(Player owner) {
        return getRika(owner) != null;
    }

    public Entity getRika(Player owner) {
        UUID id = rikaByOwner.get(owner.getUniqueId());
        if (id == null) return null;
        Entity e = Bukkit.getEntity(id);
        return (e != null && e.isValid()) ? e : null;
    }

    public void setTarget(Player owner, LivingEntity target) {
        if (target == null) return;
        if (isInRespawnGrace(target.getUniqueId())) return;
        lastTargetByOwner.put(owner.getUniqueId(), target.getUniqueId());
        lastTargetAtMs.put(owner.getUniqueId(), System.currentTimeMillis());
    }

    public void onTargetDeath(UUID ownerUuid, UUID deadTargetUuid) {
        UUID currentTarget = lastTargetByOwner.get(ownerUuid);
        if (currentTarget != null && currentTarget.equals(deadTargetUuid)) {
            lastTargetByOwner.remove(ownerUuid);
            lastTargetAtMs.remove(ownerUuid);
        }
        respawnGrace.put(deadTargetUuid, System.currentTimeMillis() + RESPAWN_GRACE_MS);
    }

    public boolean isInRespawnGrace(UUID uuid) {
        Long until = respawnGrace.get(uuid);
        if (until == null) return false;
        if (System.currentTimeMillis() > until) {
            respawnGrace.remove(uuid);
            return false;
        }
        return true;
    }

    /**
     * Convert a direction vector to a yaw angle.
     * The -180f offset corrects the ModelEngine model orientation on ArmorStands.
     */
    private float dirToYaw(Vector dir) {
        return (float) Math.toDegrees(Math.atan2(-dir.getX(), dir.getZ())) - 180f;
    }

    private void teleportWithYaw(Entity rika, Location loc) {
        rika.teleport(loc);
        if (rika instanceof ArmorStand as) {
            as.setRotation(loc.getYaw(), 0);
        }
    }

    public void returnToOwner(Player owner) {
        if (!plugin.copy().canUseCopy(owner)) {
            owner.sendMessage(plugin.cfg().prefix() + "§cYou must have §dCopy§c equipped.");
            return;
        }
        Entity rika = getRika(owner);
        if (rika == null) {
            owner.sendMessage(plugin.cfg().prefix() + "§cRika is not manifested.");
            return;
        }
        UUID u = owner.getUniqueId();
        lastTargetByOwner.remove(u);
        lastTargetAtMs.remove(u);

        Location beside = snapToGround(owner.getLocation().clone().add(1.5, 0, 1.5));
        beside.setYaw(dirToYaw(owner.getLocation().getDirection()));
        beside.setPitch(0);
        teleportWithYaw(rika, beside);
        model.playAnimation(rika, ANIM_IDLE);
        owner.sendMessage(plugin.cfg().prefix() + "§dRika has returned to your side.");
        owner.playSound(owner.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.4f);
    }

    public void trySummon(Player owner) {
        if (!plugin.copy().canUseCopy(owner)) {
            owner.sendMessage(plugin.cfg().prefix() + "§cYou must have §dCopy§c equipped to summon Rika.");
            return;
        }
        UUID u = owner.getUniqueId();

        if (plugin.cooldowns().isOnCooldown(u, "copy.rika.summon")) {
            long rem = plugin.cooldowns().remainingSeconds(u, "copy.rika.summon");
            plugin.actionbarUI().setTimer(u, "copy.rika.summon", "■", "§d", rem);
            owner.sendMessage(plugin.cfg().prefix() + "§cRika is on cooldown: §f" + TimeFmt.mmss(rem));
            return;
        }
        if (isActive(owner)) {
            owner.sendMessage(plugin.cfg().prefix() + "§7Rika is already manifested.");
            return;
        }

        Location spawnLoc = snapToGround(owner.getLocation().clone().add(1.5, 0, 1.5));
        spawnLoc.setYaw(dirToYaw(owner.getLocation().getDirection()));
        spawnLoc.setPitch(0);

        ArmorStand as = owner.getWorld().spawn(spawnLoc, ArmorStand.class, e -> {
            e.setCustomName("§dRika");
            e.setCustomNameVisible(true);
            e.setInvisible(true);
            e.setMarker(false);
            e.setSmall(false);
            e.setGravity(false);
            e.setSilent(true);
            e.setInvulnerable(true);
            e.setCanPickupItems(false);
            e.setCollidable(false);
            e.setRemoveWhenFarAway(false);
        });

        as.setRotation(spawnLoc.getYaw(), 0);
        as.getPersistentDataContainer().set(KEY_RIKA, PersistentDataType.INTEGER, 1);
        as.getPersistentDataContainer().set(KEY_OWNER, PersistentDataType.STRING, u.toString());

        rikaByOwner.put(u, as.getUniqueId());

        PlayerProfile prof = plugin.data().get(u);
        prof.rikaEntityUuid = as.getUniqueId().toString();
        plugin.data().save(u);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!as.isValid()) return;
            model.applyModel(as, MODEL_ID);
            model.playAnimation(as, ANIM_SUMMON);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (as.isValid()) model.playAnimation(as, ANIM_IDLE);
            }, 20L);
        }, 1L);

        owner.sendMessage(plugin.cfg().prefix() + "§dRika has manifested. §7(5:00)");

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!as.isValid()) return;
            dismiss(owner);
        }, ACTIVE_SECONDS * 20L);
    }

    public void dismiss(Player owner) {
        UUID u = owner.getUniqueId();
        UUID entId = rikaByOwner.remove(u);
        if (entId != null) {
            Entity e = Bukkit.getEntity(entId);
            if (e != null && e.isValid()) {
                model.removeModel(e);
                e.remove();
            }
        }
        lastTargetByOwner.remove(u);
        lastTargetAtMs.remove(u);
        plugin.cooldowns().setCooldown(u, "copy.rika.summon", SUMMON_CD_SECONDS);
        plugin.actionbarUI().setTimer(u, "copy.rika.summon", "■", "§d", SUMMON_CD_SECONDS);
        PlayerProfile prof = plugin.data().get(u);
        prof.rikaEntityUuid = null;
        plugin.data().save(u);
    }

    public void tryLoveBeam(Player owner) {
        if (!plugin.copy().canUseCopy(owner)) {
            owner.sendMessage(plugin.cfg().prefix() + "§cYou must have §dCopy§c equipped to use Beam.");
            return;
        }
        Entity rika = getRika(owner);
        if (rika == null) {
            owner.sendMessage(plugin.cfg().prefix() + "§cRika is not manifested.");
            return;
        }
        String beamKey = "copy.rika.beam";
        if (plugin.cooldowns().isOnCooldown(owner.getUniqueId(), beamKey)) {
            long rem = plugin.cooldowns().remainingSeconds(owner.getUniqueId(), beamKey);
            plugin.actionbarUI().setTimer(owner.getUniqueId(), beamKey, "■", "§d", rem);
            owner.sendMessage(plugin.cfg().prefix() + "§cBeam is on cooldown: §f" + TimeFmt.mmss(rem));
            return;
        }
        int ce = plugin.ce().get(owner.getUniqueId());
        if (ce < BEAM_COST) {
            owner.sendMessage(plugin.cfg().prefix() + "§cNot enough Cursed Energy. §7(Need 50, Have " + ce + ")");
            owner.playSound(owner.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.6f);
            return;
        }
        if (!plugin.ce().tryConsume(owner.getUniqueId(), BEAM_COST)) return;

        Vector beamDir = owner.getEyeLocation().getDirection().normalize();

        // Move Rika behind the owner, facing the beam direction, SNAPPED TO GROUND
        Location behind = owner.getLocation().clone().add(beamDir.clone().multiply(-1.6));
        behind = snapToGround(behind);
        behind.setYaw(dirToYaw(beamDir));
        behind.setPitch(0);
        teleportWithYaw(rika, behind);

        model.playAnimation(rika, ANIM_BEAM);
        spawnChargeOrb(rika, 30);

        // Store the ground location for beam origin
        final Location beamOriginLoc = behind.clone();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!rika.isValid()) return;

            // Re-snap Rika to ground in case she drifted
            Location currentGround = snapToGround(rika.getLocation());
            currentGround.setYaw(rika.getLocation().getYaw());
            currentGround.setPitch(0);
            teleportWithYaw(rika, currentGround);

            // Beam fires from Rika's mouth (ground level + 1.8)
            Location start = currentGround.clone().add(0, 1.8, 0);
            launchBeamProjectile(owner, rika, start, beamDir);
            plugin.cooldowns().setCooldown(owner.getUniqueId(), beamKey, BEAM_CD_SECONDS);
            plugin.actionbarUI().setTimer(owner.getUniqueId(), beamKey, "■", "§d", BEAM_CD_SECONDS);

            // Return Rika to idle after beam fires
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (rika.isValid()) {
                    model.playAnimation(rika, ANIM_IDLE);
                }
            }, 20L);
        }, 34L);
    }

    private void spawnChargeOrb(Entity rika, int ticks) {
        World w = rika.getWorld();
        for (int i = 0; i < ticks; i++) {
            int t = i;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!rika.isValid()) return;
                Location mouth = rika.getLocation().clone().add(0, 1.8, 0);
                double grow = 0.1 + (t / (double) ticks) * 0.75;
                w.spawnParticle(Particle.END_ROD, mouth, 14, grow, grow, grow, 0.02);
                w.spawnParticle(Particle.ELECTRIC_SPARK, mouth, 10, grow * 0.6, grow * 0.4, grow * 0.6, 0.02);
                w.spawnParticle(Particle.WITCH, mouth, 8, grow * 0.5, grow * 0.3, grow * 0.5, 0.01);
            }, i);
        }
    }

    private void launchBeamProjectile(Player owner, Entity rika, Location start, Vector direction) {
        World w = start.getWorld();
        if (w == null) return;

        ArmorStand beam = w.spawn(start, ArmorStand.class, e -> {
            e.setInvisible(true); e.setMarker(true); e.setSmall(true);
            e.setGravity(false); e.setSilent(true); e.setInvulnerable(true);
            e.setCustomNameVisible(false);
        });
        beam.getPersistentDataContainer().set(KEY_BEAM, PersistentDataType.INTEGER, 1);

        w.playSound(start, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 1.3f);
        w.playSound(start, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.25f, 2.0f);

        Particle.DustOptions pinkDust = new Particle.DustOptions(Color.fromRGB(255, 80, 255), 2.2f);
        Particle.DustOptions whiteDust = new Particle.DustOptions(Color.fromRGB(255, 255, 255), 1.6f);

        Set<UUID> alreadyHit = new HashSet<>();
        alreadyHit.add(owner.getUniqueId());
        alreadyHit.add(rika.getUniqueId());
        alreadyHit.add(beam.getUniqueId());

        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            if (!beam.isValid()) { task.cancel(); return; }
            Location current = beam.getLocation();
            double traveled = current.distance(start);
            if (traveled >= BEAM_RANGE) { spawnImpactEffect(current); beam.remove(); task.cancel(); return; }

            Vector step = direction.clone().multiply(BEAM_SPEED);
            Location next = current.clone().add(step);
            beam.teleport(next);

            int trailSteps = (int) Math.ceil(BEAM_SPEED / 0.5);
            for (int i = 0; i <= trailSteps; i++) {
                double t = i / (double) trailSteps;
                Location tp = current.clone().add(step.clone().multiply(t));
                w.spawnParticle(Particle.END_ROD, tp, 4, 0.08, 0.08, 0.08, 0.01);
                w.spawnParticle(Particle.ELECTRIC_SPARK, tp, 3, 0.06, 0.06, 0.06, 0.01);
                w.spawnParticle(Particle.DUST, tp, 4, 0.12, 0.12, 0.12, 0, pinkDust);
                w.spawnParticle(Particle.DUST, tp, 2, 0.08, 0.08, 0.08, 0, whiteDust);
                w.spawnParticle(Particle.WITCH, tp, 2, 0.10, 0.10, 0.10, 0.01);
            }
            spawnBoreRing(w, next, direction, TRENCH_RADIUS);
            if (traveled % 8 < BEAM_SPEED) w.playSound(next, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.6f, 1.8f);

            for (LivingEntity le : w.getLivingEntities()) {
                if (!le.isValid() || alreadyHit.contains(le.getUniqueId()) || le instanceof ArmorStand) continue;
                Location ec = le.getLocation().clone().add(0, Math.max(0.9, le.getHeight() * 0.5), 0);
                if (distanceToSegment(ec, current, next) <= BEAM_HIT_RADIUS) {
                    alreadyHit.add(le.getUniqueId());
                    le.damage(BEAM_DAMAGE, owner); le.setFireTicks(80);
                    w.spawnParticle(Particle.EXPLOSION, le.getLocation().clone().add(0, 1.0, 0), 1, 0, 0, 0, 0);
                    w.spawnParticle(Particle.ELECTRIC_SPARK, le.getLocation().clone().add(0, 1.0, 0), 20, 0.5, 0.5, 0.5, 0.05);
                    w.playSound(le.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.2f);
                }
            }
            int carveSteps = (int) Math.ceil(BEAM_SPEED);
            for (int i = 0; i <= carveSteps; i++) {
                double ct = i / (double) carveSteps;
                Location cp = current.clone().add(step.clone().multiply(ct));
                carveSphere(cp);
            }
        }, 0L, 1L);
    }

    private void carveSphere(Location center) {
        World w = center.getWorld(); if (w == null) return;
        int cx = center.getBlockX(); int cy = center.getBlockY(); int cz = center.getBlockZ();
        int r = TRENCH_RADIUS; int rSq = r * r;
        for (int dx = -r; dx <= r; dx++) for (int dy = -r; dy <= r; dy++) for (int dz = -r; dz <= r; dz++) {
            if ((dx * dx + dy * dy + dz * dz) > rSq) continue;
            int bx = cx + dx; int by = cy + dy; int bz = cz + dz;
            if (by <= w.getMinHeight() || by >= w.getMaxHeight()) continue;
            Block b = w.getBlockAt(bx, by, bz); Material mat = b.getType();
            if (mat == Material.BEDROCK || mat.isAir()) continue;
            if (b.getState() instanceof org.bukkit.block.Container) continue;
            b.setType(Material.AIR, false);
        }
    }

    private void spawnBoreRing(World w, Location center, Vector beamDir, int radius) {
        Vector up = new Vector(0, 1, 0);
        if (Math.abs(beamDir.dot(up)) > 0.99) up = new Vector(1, 0, 0);
        Vector right = beamDir.clone().crossProduct(up).normalize();
        Vector perpUp = right.clone().crossProduct(beamDir).normalize();
        Particle.DustOptions ringDust = new Particle.DustOptions(Color.fromRGB(200, 50, 255), 1.8f);
        int points = 24;
        for (int i = 0; i < points; i++) {
            double angle = (Math.PI * 2.0) * (i / (double) points);
            double rx = Math.cos(angle) * radius; double ry = Math.sin(angle) * radius;
            Location ringPoint = center.clone().add(right.clone().multiply(rx)).add(perpUp.clone().multiply(ry));
            w.spawnParticle(Particle.DUST, ringPoint, 1, 0, 0, 0, 0, ringDust);
            w.spawnParticle(Particle.ELECTRIC_SPARK, ringPoint, 1, 0.1, 0.1, 0.1, 0.01);
        }
    }

    private double distanceToSegment(Location point, Location segStart, Location segEnd) {
        Vector ap = point.toVector().subtract(segStart.toVector());
        Vector ab = segEnd.toVector().subtract(segStart.toVector());
        double abLenSq = ab.lengthSquared(); if (abLenSq < 0.0001) return ap.length();
        double t = Math.max(0.0, Math.min(1.0, ap.dot(ab) / abLenSq));
        Vector closest = segStart.toVector().add(ab.multiply(t));
        return point.toVector().distance(closest);
    }

    private void spawnImpactEffect(Location loc) {
        World w = loc.getWorld(); if (w == null) return;
        w.spawnParticle(Particle.EXPLOSION, loc, 2, 0.3, 0.3, 0.3, 0);
        w.spawnParticle(Particle.ELECTRIC_SPARK, loc, 30, 0.8, 0.8, 0.8, 0.05);
        w.spawnParticle(Particle.END_ROD, loc, 20, 0.6, 0.6, 0.6, 0.04);
        w.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.9f);
    }

    private Location snapToGround(Location loc) {
        World w = loc.getWorld(); if (w == null) return loc;
        int x = loc.getBlockX(); int z = loc.getBlockZ(); int startY = loc.getBlockY();
        for (int y = startY + 5; y >= Math.max(w.getMinHeight(), startY - 15); y--) {
            Block block = w.getBlockAt(x, y, z); Block above = w.getBlockAt(x, y + 1, z);
            if (!block.getType().isAir() && !block.isLiquid() && above.getType().isAir()) {
                Location ground = new Location(w, loc.getX(), y + 1.0, loc.getZ());
                ground.setYaw(loc.getYaw()); ground.setPitch(loc.getPitch()); return ground;
            }
        }
        int highestY = w.getHighestBlockYAt(x, z);
        Location ground = new Location(w, loc.getX(), highestY + 1.0, loc.getZ());
        ground.setYaw(loc.getYaw()); ground.setPitch(loc.getPitch()); return ground;
    }

    private double lerpY(double currentY, double targetY) {
        double diff = targetY - currentY; if (Math.abs(diff) < 0.05) return targetY;
        return currentY + diff * Y_LERP_SPEED;
    }

    private void tick() {
        long now = System.currentTimeMillis();
        respawnGrace.entrySet().removeIf(e -> now > e.getValue());

        for (Map.Entry<UUID, UUID> entry : rikaByOwner.entrySet()) {
            UUID ownerId = entry.getKey(); UUID rikaId = entry.getValue();
            Player owner = Bukkit.getPlayer(ownerId);
            if (owner == null || !owner.isOnline()) continue;
            if (!plugin.copy().canUseCopy(owner)) { dismiss(owner); continue; }
            Entity rika = Bukkit.getEntity(rikaId);
            if (rika == null || !rika.isValid()) continue;

            // Hard teleport if too far
            if (rika.getLocation().distance(owner.getLocation()) > HARD_LIMIT) {
                Location beside = snapToGround(owner.getLocation().clone().add(1.0, 0.0, 1.0));
                beside.setYaw(dirToYaw(owner.getLocation().getDirection()));
                beside.setPitch(0);
                teleportWithYaw(rika, beside); continue;
            }

            // Auto-heal owner
            if (owner.getHealth() <= HEAL_THRESHOLD_HP) {
                String key = "copy.rika.rct";
                if (!plugin.cooldowns().isOnCooldown(ownerId, key)) {
                    owner.addPotionEffect(new org.bukkit.potion.PotionEffect(
                            org.bukkit.potion.PotionEffectType.REGENERATION, 5 * 20, 4, false, false, true));
                    plugin.cooldowns().setCooldown(ownerId, key, HEAL_CD_SECONDS);
                    owner.getWorld().playSound(owner.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.8f, 1.7f);
                }
            }

            // Resolve target
            UUID targetId = lastTargetByOwner.get(ownerId);
            long lastAt = lastTargetAtMs.getOrDefault(ownerId, 0L);
            LivingEntity target = null;
            if (targetId != null && (now - lastAt) <= TARGET_TIMEOUT_MS) {
                if (isInRespawnGrace(targetId)) {
                    lastTargetByOwner.remove(ownerId); lastTargetAtMs.remove(ownerId);
                } else {
                    Entity ent = Bukkit.getEntity(targetId);
                    if (ent instanceof LivingEntity le && le.isValid()) {
                        if (le instanceof Player tp && tp.isDead()) onTargetDeath(ownerId, targetId);
                        else target = le;
                    }
                }
            }

            Location rl = rika.getLocation();

            if (target != null) {
                // === CHASE TARGET ===
                Location tl = target.getLocation();
                Vector toTarget = tl.toVector().subtract(rl.toVector()); toTarget.setY(0);
                double horizDist = toTarget.length();
                Location desired;
                if (horizDist > ATTACK_STOP_DISTANCE) {
                    double moveAmount = Math.min(CHASE_SPEED, horizDist - ATTACK_STOP_DISTANCE);
                    if (moveAmount > 0.01) desired = rl.clone().add(toTarget.normalize().multiply(moveAmount));
                    else desired = rl.clone();
                } else desired = rl.clone();

                Location groundAt = snapToGround(desired);
                double smoothY = lerpY(rl.getY(), groundAt.getY());
                desired.setY(smoothY);

                // Face the target
                Vector faceDir = tl.toVector().subtract(desired.toVector()); faceDir.setY(0);
                if (faceDir.lengthSquared() > 0.01) {
                    faceDir.normalize();
                    desired.setYaw(dirToYaw(faceDir));
                    desired.setPitch(0);
                }
                teleportWithYaw(rika, desired);

                // Attack if in range
                double attackDist = rika.getLocation().distance(target.getLocation());
                if (attackDist <= ATTACK_RANGE) {
                    String atkKey = "copy.rika.attack";
                    if (!plugin.cooldowns().isOnCooldown(ownerId, atkKey)) {
                        model.playAnimation(rika, ANIM_ATTACK);
                        target.damage(6.0, owner);
                        plugin.cooldowns().setCooldown(ownerId, atkKey, 1);
                    }
                }
            } else {
                // === FOLLOW OWNER ===
                Location ol = owner.getLocation();
                Vector toOwner = ol.toVector().subtract(rl.toVector()); toOwner.setY(0);
                double horizDist = toOwner.length();

                if (horizDist > FOLLOW_DISTANCE) {
                    Vector step = toOwner.normalize().multiply(Math.min(FOLLOW_SPEED, horizDist - FOLLOW_DISTANCE + 0.1));
                    Location desired = rl.clone().add(step);
                    Location groundAt = snapToGround(desired);
                    double smoothY = lerpY(rl.getY(), groundAt.getY());
                    desired.setY(smoothY);

                    // Face the direction of movement (towards owner)
                    desired.setYaw(dirToYaw(toOwner.normalize()));
                    desired.setPitch(0);
                    teleportWithYaw(rika, desired);
                } else {
                    // Standing still near owner — face same direction as owner
                    Location adjusted = rl.clone();
                    adjusted.setYaw(dirToYaw(owner.getLocation().getDirection()));
                    adjusted.setPitch(0);
                    Location groundAt = snapToGround(adjusted);
                    double smoothY = lerpY(rl.getY(), groundAt.getY());
                    adjusted.setY(smoothY);
                    teleportWithYaw(rika, adjusted);
                }
            }
        }
    }

    public boolean isRikaEntity(Entity e) {
        if (e == null || !e.isValid()) return false;
        Integer v = e.getPersistentDataContainer().get(KEY_RIKA, PersistentDataType.INTEGER);
        return v != null && v == 1;
    }

    public UUID ownerOf(Entity rika) {
        if (!isRikaEntity(rika)) return null;
        String s = rika.getPersistentDataContainer().get(KEY_OWNER, PersistentDataType.STRING);
        if (s == null) return null;
        try { return UUID.fromString(s); } catch (Exception ex) { return null; }
    }
}