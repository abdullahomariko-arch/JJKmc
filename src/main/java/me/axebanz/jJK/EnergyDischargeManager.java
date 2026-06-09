package me.axebanz.jJK;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class EnergyDischargeManager {

    private final JJKCursedToolsPlugin plugin;

    private static final Particle.DustOptions BLUE_CORE =
            new Particle.DustOptions(Color.fromRGB(0, 170, 255), 1.1f);
    private static final Particle.DustOptions BLUE_HIT =
            new Particle.DustOptions(Color.fromRGB(0, 150, 255), 1.6f);
    private static final Particle.DustOptions BLUE_STRIKE =
            new Particle.DustOptions(Color.fromRGB(0, 150, 255), 1.5f);
    private static final Particle.DustOptions BEAM_SWIRL =
            new Particle.DustOptions(Color.fromRGB(0, 190, 255), 1.0f);

    private static final int BLAST_CE_LOW = 2;
    private static final int BLAST_CE_MEDIUM = 6;
    private static final int BLAST_CE_HIGH = 12;

    private final Map<UUID, Long> trackingChargeStart = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> trackingChargeTasks = new ConcurrentHashMap<>();
    private final Map<UUID, GraniteBlastSession> blastSessions = new ConcurrentHashMap<>();

    private final Map<UUID, Integer> heatValues = new ConcurrentHashMap<>();
    private final Map<UUID, Long> overheatUntil = new ConcurrentHashMap<>();
    private final Set<UUID> overheatNotified = ConcurrentHashMap.newKeySet();

    private BukkitTask heatDecayTask;

    private record BeamConfig(float thickness, float length, int durationTicks, double damage, int ceCost) {}

    public EnergyDischargeManager(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
        startHeatDecayTask();
    }

    public boolean hasTechnique(Player p) {
        String id = plugin.techniqueManager().getAssignedId(p.getUniqueId());
        return "energy_discharge".equalsIgnoreCase(id);
    }

    public void applyStrikePassive(Player attacker, LivingEntity victim, double baseDamage, EntityDamageByEntityEvent event) {
        event.setDamage(baseDamage * 1.5);
        Location loc = victim.getLocation().add(0, 1, 0);
        World w = attacker.getWorld();
        w.spawnParticle(Particle.DUST, loc, 8, 0.4, 0.4, 0.4, 0, BLUE_STRIKE);
        w.spawnParticle(Particle.END_ROD, loc, 4, 0.3, 0.3, 0.3, 0.05);
    }

    public void applyDamageReduction(EntityDamageEvent event) {
        event.setDamage(event.getDamage() * 0.80);
    }

    public boolean isLocked(UUID uuid) {
        return false;
    }

    private void startHeatDecayTask() {
        if (heatDecayTask != null) heatDecayTask.cancel();

        heatDecayTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();

            for (Player p : Bukkit.getOnlinePlayers()) {
                UUID uuid = p.getUniqueId();

                Long overheatEnd = overheatUntil.get(uuid);
                if (overheatEnd != null) {
                    if (now >= overheatEnd) {
                        overheatUntil.remove(uuid);
                        heatValues.put(uuid, 0);
                        if (overheatNotified.remove(uuid)) {
                            p.sendMessage(plugin.cfg().prefix() + "§bGranite Blast is ready again.");
                        }
                    }
                    continue;
                }

                int current = heatValues.getOrDefault(uuid, 0);
                if (current > 0) {
                    heatValues.put(uuid, Math.max(0, current - 2));
                }
            }
        }, 20L, 20L);
    }

    public int getHeatPercent(UUID uuid) {
        return Math.max(0, Math.min(100, heatValues.getOrDefault(uuid, 0)));
    }

    public boolean isOverheated(UUID uuid) {
        Long until = overheatUntil.get(uuid);
        return until != null && System.currentTimeMillis() < until;
    }

    private void addHeat(Player p, int amount) {
        UUID uuid = p.getUniqueId();
        if (isOverheated(uuid)) return;

        int current = heatValues.getOrDefault(uuid, 0);
        int next = Math.min(100, current + amount);
        heatValues.put(uuid, next);

        if (next >= 100) {
            overheatUntil.put(uuid, System.currentTimeMillis() + 10_000L);
            if (overheatNotified.add(uuid)) {
                p.sendMessage(plugin.cfg().prefix() + "§c❗ You have overheated. Wait 10 seconds.");
            }
        }
    }

    public void startTrackingCharge(Player p) {
        if (!hasTechnique(p)) {
            p.sendMessage(plugin.cfg().prefix() + "§cYou don't have §b⚡ Energy Discharge§c equipped.");
            return;
        }

        UUID uuid = p.getUniqueId();

        if (isOverheated(uuid)) {
            return;
        }

        if (trackingChargeStart.containsKey(uuid)) {
            fireTrackingBeam(p, currentTrackingChargePct(uuid));
            return;
        }

        if (plugin.cooldowns().isOnCooldown(uuid, "ed.tracking")) {
            long rem = plugin.cooldowns().remainingSeconds(uuid, "ed.tracking");
            p.sendMessage(plugin.cfg().prefix() + "§cTracking Beam on cooldown: §f" + rem + "s");
            return;
        }

        trackingChargeStart.put(uuid, System.currentTimeMillis());

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!p.isOnline()) {
                cancelTrackingCharge(uuid);
                return;
            }

            int pct = currentTrackingChargePct(uuid);
            int filled = pct / 10;
            String bar = "§b" + "│".repeat(filled) + "§7" + "│".repeat(10 - filled);
            p.sendActionBar("§b⚡ Tracking Beam: [" + bar + "] " + pct + "%");

            if (pct >= 100) {
                fireTrackingBeam(p, 100);
            }
        }, 0L, 4L);

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

        addHeat(p, chargePct >= 100 ? 40 : 20);

        plugin.cooldowns().setCooldown(uuid, "ed.tracking", 5);
        p.sendActionBar("§b⚡ Tracking Beam: §7Fired!");

        LivingEntity target = findTarget(p, range);
        if (target == null) {
            p.sendMessage(plugin.cfg().prefix() + "§7No target in range.");
            return;
        }

        launchHomingBeam(p, target, range, damage);
    }

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

            Location targetCenter = target.getLocation().add(0, 1, 0);
            double dx = targetCenter.getX() - pos[0];
            double dy = targetCenter.getY() - pos[1];
            double dz = targetCenter.getZ() - pos[2];
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

            if (dist > 0.01) {
                double factor = 0.15;
                velocity[0] += (dx / dist) * factor;
                velocity[1] += (dy / dist) * factor;
                velocity[2] += (dz / dist) * factor;
            }

            double vLen = Math.sqrt(velocity[0] * velocity[0] + velocity[1] * velocity[1] + velocity[2] * velocity[2]);
            if (vLen > 0.01) {
                velocity[0] = (velocity[0] / vLen) * speed;
                velocity[1] = (velocity[1] / vLen) * speed;
                velocity[2] = (velocity[2] / vLen) * speed;
            }

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

    public void startBlastCharge(Player p) {
        if (!hasTechnique(p)) {
            p.sendMessage(plugin.cfg().prefix() + "§cYou don't have §b⚡ Energy Discharge§c equipped.");
            return;
        }

        UUID uuid = p.getUniqueId();

        if (isOverheated(uuid)) {
            return;
        }

        GraniteBlastSession existing = blastSessions.get(uuid);
        if (existing != null && existing.isCharging()) return;

        if (plugin.cooldowns().isOnCooldown(uuid, "ed.blast")) {
            long rem = plugin.cooldowns().remainingSeconds(uuid, "ed.blast");
            p.sendMessage(plugin.cfg().prefix() + "§cGranite Blast on cooldown: §f" + rem + "s");
            return;
        }

        GraniteBlastSession session = new GraniteBlastSession();
        session.startCharging();
        blastSessions.put(uuid, session);

        Location spawnLoc = p.getLocation().add(0, 2.5, 0);
        ItemDisplay chargeDisplay = (ItemDisplay) p.getWorld().spawnEntity(spawnLoc, EntityType.ITEM_DISPLAY);

        ItemStack chargeItem = new ItemStack(Material.PAPER);
        ItemMeta chargeMeta = chargeItem.getItemMeta();
        if (chargeMeta != null) {
            chargeMeta.setItemModel(new NamespacedKey("mybeam", "granite_charge"));
            chargeItem.setItemMeta(chargeMeta);
        }

        chargeDisplay.setItemStack(chargeItem);
        chargeDisplay.setBrightness(new Display.Brightness(15, 15));
        chargeDisplay.setTeleportDuration(1);
        chargeDisplay.setTransformation(new Transformation(
                new Vector3f(0f, 0f, 0f),
                new Quaternionf(),
                new Vector3f(0.3f, 0.3f, 0.3f),
                new Quaternionf()
        ));
        session.chargeVisualEntity = chargeDisplay;

        BukkitTask chargeTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!p.isOnline()) {
                cancelBlastCharge(p);
                return;
            }

            session.updateChargePercent();
            int pct = session.getChargePercent();

            Location headLoc = p.getLocation().add(0, 2.5, 0);
            if (session.chargeVisualEntity != null && session.chargeVisualEntity.isValid()) {
                session.chargeVisualEntity.teleport(headLoc);

                float scale = 0.3f + (0.7f * pct / 100f);
                session.chargeVisualEntity.setTransformation(new Transformation(
                        new Vector3f(0f, 0f, 0f),
                        new Quaternionf(),
                        new Vector3f(scale, scale, scale),
                        new Quaternionf()
                ));
            }

            if (pct >= 100) {
                releaseBlastCharge(p);
            }
        }, 0L, 1L);

        session.chargingTask = chargeTask;
    }

    public void releaseBlastCharge(Player p) {
        UUID uuid = p.getUniqueId();
        GraniteBlastSession session = blastSessions.get(uuid);
        if (session == null || !session.isCharging()) return;

        GraniteBlastSession.ChargeTier tier = session.release();

        if (session.chargingTask != null) {
            session.chargingTask.cancel();
            session.chargingTask = null;
        }

        if (session.chargeVisualEntity != null && session.chargeVisualEntity.isValid()) {
            ItemDisplay orb = session.chargeVisualEntity;
            session.chargeVisualEntity = null;

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (orb.isValid()) orb.remove();
            }, tier == GraniteBlastSession.ChargeTier.LOW ? 0L : 10L);
        }

        switch (tier) {
            case LOW -> fireBlastProjectile(p, session);
            case MEDIUM -> fireBlastBeam(p, session, getMediumConfig());
            case HIGH -> fireBlastBeam(p, session, getHighConfig());
        }
    }

    public void cancelBlastCharge(Player p) {
        UUID uuid = p.getUniqueId();
        GraniteBlastSession session = blastSessions.remove(uuid);
        if (session == null) return;

        session.cancel();
        session.cleanup();
    }

    private BeamConfig getMediumConfig() {
        return new BeamConfig(3.0f, 34f, 55, 14.0, BLAST_CE_MEDIUM);
    }

    private BeamConfig getHighConfig() {
        return new BeamConfig(5.5f, 62f, 95, 28.0, BLAST_CE_HIGH);
    }

    private void fireBlastProjectile(Player p, GraniteBlastSession session) {
        UUID uuid = p.getUniqueId();

        if (!plugin.ce().tryConsume(uuid, BLAST_CE_LOW)) {
            p.sendMessage(plugin.cfg().prefix() + "§cNot enough Cursed Energy.");
            blastSessions.remove(uuid);
            session.cleanup();
            return;
        }

        addHeat(p, 15);
        plugin.cooldowns().setCooldown(uuid, "ed.blast", 15);

        Location eyeLoc = p.getEyeLocation();
        Vector fwd = eyeLoc.getDirection().normalize();

        Location spawnLoc = eyeLoc.clone()
                .add(fwd.multiply(1.6))
                .subtract(0, 0.28, 0);

        ItemDisplay proj = (ItemDisplay) p.getWorld().spawnEntity(spawnLoc, EntityType.ITEM_DISPLAY);
        ItemStack projItem = new ItemStack(Material.PAPER);
        ItemMeta projMeta = projItem.getItemMeta();
        if (projMeta != null) {
            projMeta.setItemModel(new NamespacedKey("mybeam", "granite_blast2"));
            projItem.setItemMeta(projMeta);
        }

        proj.setItemStack(projItem);
        proj.setBrightness(new Display.Brightness(15, 15));
        proj.setTransformation(new Transformation(
                new Vector3f(0f, 0f, 0f),
                new Quaternionf(),
                new Vector3f(0.7f, 0.7f, 1.8f),
                new Quaternionf()
        ));
        proj.setRotation(eyeLoc.getYaw(), eyeLoc.getPitch());
        session.beamEntity = proj;

        Vector dir = eyeLoc.getDirection().normalize();
        double[] pos = {spawnLoc.getX(), spawnLoc.getY(), spawnLoc.getZ()};
        double[] distTravelled = {0.0};
        double speed = 1.5;
        double maxDist = 20.0;
        double damage = 4.0;

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!p.isOnline() || !proj.isValid()) {
                session.cleanup();
                blastSessions.remove(uuid);
                return;
            }

            pos[0] += dir.getX() * speed;
            pos[1] += dir.getY() * speed;
            pos[2] += dir.getZ() * speed;
            distTravelled[0] += speed;

            Location newLoc = new Location(p.getWorld(), pos[0], pos[1], pos[2]);
            proj.teleport(newLoc);

            if (distTravelled[0] >= maxDist) {
                proj.remove();
                session.cleanup();
                blastSessions.remove(uuid);
                return;
            }

            if (newLoc.getBlock().getType().isSolid()) {
                proj.remove();
                session.cleanup();
                blastSessions.remove(uuid);
                return;
            }

            for (Entity ent : newLoc.getWorld().getNearbyEntities(newLoc, 0.8, 0.8, 0.8)) {
                if (!(ent instanceof LivingEntity le)) continue;
                if (ent.getUniqueId().equals(uuid)) continue;

                le.damage(damage, p);
                spawnHitExplosion(p.getWorld(), newLoc);
                proj.remove();
                session.cleanup();
                blastSessions.remove(uuid);
                return;
            }
        }, 0L, 1L);

        session.projectileTask = task;
    }

    private void fireBlastBeam(Player p, GraniteBlastSession session, BeamConfig cfg) {
        UUID uuid = p.getUniqueId();

        if (!plugin.ce().tryConsume(uuid, cfg.ceCost())) {
            p.sendMessage(plugin.cfg().prefix() + "§cNot enough Cursed Energy.");
            blastSessions.remove(uuid);
            session.cleanup();
            return;
        }

        if (cfg.ceCost() <= BLAST_CE_MEDIUM) {
            addHeat(p, 35);
        } else {
            addHeat(p, 60);
        }

        long cooldown = cfg.durationTicks() <= 55 ? 30 : 60;
        plugin.cooldowns().setCooldown(uuid, "ed.blast", cooldown);

        Location eyeLoc = p.getEyeLocation();
        Vector fwd = eyeLoc.getDirection().normalize();

        Location beamSpawn = eyeLoc.clone()
                .add(fwd.clone().multiply(2.2))
                .subtract(0, 0.32, 0);

        ItemDisplay beam = (ItemDisplay) p.getWorld().spawnEntity(beamSpawn, EntityType.ITEM_DISPLAY);
        ItemStack beamItem = new ItemStack(Material.PAPER);
        ItemMeta beamMeta = beamItem.getItemMeta();
        if (beamMeta != null) {
            beamMeta.setItemModel(new NamespacedKey("mybeam", "granite_blast2"));
            beamItem.setItemMeta(beamMeta);
        }

        beam.setItemStack(beamItem);
        beam.setBrightness(new Display.Brightness(15, 15));
        beam.setTeleportDuration(1);
        beam.setTransformation(new Transformation(
                new Vector3f(0f, 0f, 0f),
                new Quaternionf(),
                new Vector3f(cfg.thickness(), cfg.thickness(), cfg.length()),
                new Quaternionf()
        ));
        beam.setRotation(beamSpawn.getYaw(), beamSpawn.getPitch());
        session.beamEntity = beam;

        int[] tickCount = {0};
        double halfWidth = cfg.thickness() / 2.0 + 0.5;
        Set<UUID> hitThisTick = new HashSet<>();
        double damagePerTick = cfg.damage() / 4.0;

        BukkitTask beamTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            tickCount[0]++;

            if (!p.isOnline() || !beam.isValid() || tickCount[0] > cfg.durationTicks()) {
                beam.remove();
                session.cleanup();
                blastSessions.remove(uuid);
                return;
            }

            Location eye = p.getEyeLocation();
            Vector forward = eye.getDirection().normalize();

            Location beamOrigin = eye.clone()
                    .add(forward.multiply(2.2))
                    .subtract(0, 0.32, 0);

            beam.teleport(beamOrigin);
            beam.setRotation(eye.getYaw(), eye.getPitch());

            Vector beamDir = eye.getDirection().normalize();
            hitThisTick.clear();

            for (double d = 0.5; d <= cfg.length(); d += 0.5) {
                Location checkLoc = beamOrigin.clone().add(beamDir.clone().multiply(d));
                if (checkLoc.getBlock().getType().isSolid()) break;

                for (Entity ent : checkLoc.getWorld().getNearbyEntities(checkLoc, halfWidth, halfWidth, halfWidth)) {
                    if (!(ent instanceof LivingEntity le)) continue;
                    if (ent.getUniqueId().equals(uuid)) continue;
                    if (hitThisTick.contains(ent.getUniqueId())) continue;

                    hitThisTick.add(ent.getUniqueId());
                    le.damage(damagePerTick, p);
                }
            }
        }, 0L, 1L);
        session.beamTask = beamTask;

        double[] spiralAngle = {0.0};
        BukkitTask spiralTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!p.isOnline() || !beam.isValid()) return;

            Location eye = p.getEyeLocation();
            Vector beamDir = eye.getDirection().normalize();

            Location spiralOrigin = eye.clone()
                    .add(beamDir.clone().multiply(2.2))
                    .subtract(0, 0.32, 0);

            Vector perp1;
            Vector crossed = beamDir.clone().crossProduct(new Vector(0, 1, 0));
            if (crossed.lengthSquared() < 0.01) {
                perp1 = beamDir.clone().crossProduct(new Vector(1, 0, 0)).normalize();
            } else {
                perp1 = crossed.normalize();
            }
            Vector perp2 = beamDir.clone().crossProduct(perp1).normalize();

            double radius = Math.max(1.0, cfg.thickness() * 0.45);
            spiralAngle[0] += 0.7;

            for (double d = 0.0; d <= cfg.length(); d += 1.0) {
                double angle = spiralAngle[0] + d * 0.8;
                double ox = Math.cos(angle) * radius;
                double oy = Math.sin(angle) * radius;

                Location spiralLoc = spiralOrigin.clone()
                        .add(beamDir.clone().multiply(d))
                        .add(perp1.clone().multiply(ox))
                        .add(perp2.clone().multiply(oy));

                spiralLoc.getWorld().spawnParticle(Particle.DUST, spiralLoc, 1, 0, 0, 0, 0, BEAM_SWIRL);
            }
        }, 0L, 1L);
        session.spiralTask = spiralTask;
    }

    public void cmdTracking(Player p) {
        startTrackingCharge(p);
    }

    public void cmdBlast(Player p) {
        UUID uuid = p.getUniqueId();
        GraniteBlastSession session = blastSessions.get(uuid);
        if (session != null && session.isCharging()) {
            releaseBlastCharge(p);
        } else {
            startBlastCharge(p);
        }
    }

    private LivingEntity findTarget(Player player, double range) {
        List<Entity> nearby = new ArrayList<>(player.getNearbyEntities(range, range, range));
        Location eye = player.getEyeLocation();
        Vector dir = eye.getDirection().normalize();

        LivingEntity bestLooking = null;
        double bestDot = 0.7;

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

    public GraniteBlastSession getBlastSession(UUID uuid) {
        return blastSessions.get(uuid);
    }

    public void cleanup(UUID uuid) {
        cancelTrackingCharge(uuid);
        GraniteBlastSession session = blastSessions.remove(uuid);
        if (session != null) {
            session.cancel();
            session.cleanup();
        }
    }
}