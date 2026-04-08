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

/**
 * Energy Discharge Technique — highest CE output.
 *
 * Passive  — Enhanced Strikes: 1.5x melee damage + blue particles, 20% damage reduction.
 * Ability 1 — Tracking Beams: chargeable homing beam, up to 2 seconds for full power.
 * Ability 2 — Granite Blast: hold-and-release with 3 tiers (LOW projectile, MEDIUM/HIGH beam).
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
    // Sandy brown for granite spiral particles
    private static final Particle.DustOptions GRANITE_SPIRAL = new Particle.DustOptions(Color.fromRGB(205, 133, 63), 0.8f);

    // Tracking beam charge state: UUID → charge start time (ms)
    private final Map<UUID, Long> trackingChargeStart = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> trackingChargeTasks = new ConcurrentHashMap<>();

    // ── Granite Blast sessions ────────────────────────────────────────────────
    private final Map<UUID, GraniteBlastSession> blastSessions = new ConcurrentHashMap<>();

    // ── Beam config inner record ──────────────────────────────────────────────
    private record BeamConfig(float thickness, float length, int durationTicks, double damage, int ceCost) {}

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

    /** Whether a player is currently movement-locked (no-op in the new Granite Blast system). */
    public boolean isLocked(UUID uuid) {
        return false;
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
    // ABILITY 2 — GRANITE BLAST (hold-and-release, 3 tiers)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Begins Granite Blast charging for the player.
     * If the player is already charging, this call is ignored (use releaseBlastCharge to fire).
     */
    public void startBlastCharge(Player p) {
        if (!hasTechnique(p)) {
            p.sendMessage(plugin.cfg().prefix() + "§cYou don't have §b⚡ Energy Discharge§c equipped.");
            return;
        }
        UUID uuid = p.getUniqueId();

        // Already charging — ignore (release is triggered separately)
        if (blastSessions.containsKey(uuid) && blastSessions.get(uuid).isCharging()) return;

        if (plugin.cooldowns().isOnCooldown(uuid, "ed.blast")) {
            long rem = plugin.cooldowns().remainingSeconds(uuid, "ed.blast");
            p.sendMessage(plugin.cfg().prefix() + "§cGranite Blast on cooldown: §f" + rem + "s");
            return;
        }

        GraniteBlastSession session = new GraniteBlastSession();
        session.startCharging();
        blastSessions.put(uuid, session);

        // Spawn charge visual above the player's head
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

        // Repeating task: update percent, move visual, update scale, show glyph bar
        BukkitTask chargeTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!p.isOnline()) {
                cancelBlastCharge(p);
                return;
            }
            session.updateChargePercent();
            int pct = session.getChargePercent();

            // Move charge visual above player every tick
            Location headLoc = p.getLocation().add(0, 2.5, 0);
            if (session.chargeVisualEntity != null && session.chargeVisualEntity.isValid()) {
                session.chargeVisualEntity.teleport(headLoc);
                // Scale from 0.3 to 1.0 based on percent
                float scale = 0.3f + (0.7f * pct / 100f);
                session.chargeVisualEntity.setTransformation(new Transformation(
                        new Vector3f(0f, 0f, 0f),
                        new Quaternionf(),
                        new Vector3f(scale, scale, scale),
                        new Quaternionf()
                ));
            }

            GlyphChargeBar.showChargeBar(p, pct);

            // Auto-fire at full charge
            if (pct >= 100) {
                releaseBlastCharge(p);
            }
        }, 0L, 1L);

        session.chargingTask = chargeTask;
    }

    /**
     * Releases Granite Blast, firing based on the current charge tier.
     * If the player is not charging, this call is ignored.
     */
    public void releaseBlastCharge(Player p) {
        UUID uuid = p.getUniqueId();
        GraniteBlastSession session = blastSessions.get(uuid);
        if (session == null || !session.isCharging()) return;

        GraniteBlastSession.ChargeTier tier = session.release();

        // Stop charge task and clear UI
        if (session.chargingTask != null) {
            session.chargingTask.cancel();
            session.chargingTask = null;
        }
        GlyphChargeBar.clearChargeBar(p);

        // Remove charge visual immediately for LOW, keep briefly for MEDIUM/HIGH
        if (tier == GraniteBlastSession.ChargeTier.LOW) {
            if (session.chargeVisualEntity != null && session.chargeVisualEntity.isValid()) {
                session.chargeVisualEntity.remove();
            }
            session.chargeVisualEntity = null;
        } else {
            // Remove charge orb after a short delay for medium/high
            if (session.chargeVisualEntity != null && session.chargeVisualEntity.isValid()) {
                ItemDisplay orb = session.chargeVisualEntity;
                session.chargeVisualEntity = null;
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (orb.isValid()) orb.remove();
                }, 10L);
            }
        }

        switch (tier) {
            case LOW    -> fireBlastProjectile(p, session);
            case MEDIUM -> fireBlastBeam(p, session, getMediumConfig(uuid));
            case HIGH   -> fireBlastBeam(p, session, getHighConfig(uuid));
        }
    }

    /**
     * Cancels an active Granite Blast charge without firing.
     */
    public void cancelBlastCharge(Player p) {
        UUID uuid = p.getUniqueId();
        GraniteBlastSession session = blastSessions.remove(uuid);
        if (session == null) return;
        GlyphChargeBar.clearChargeBar(p);
        session.cancel();
        session.cleanup();
    }

    // ── Beam configs ─────────────────────────────────────────────────────────

    private BeamConfig getMediumConfig(UUID uuid) {
        int ceCost = (int) (plugin.ce().max(uuid) * 0.25);
        return new BeamConfig(0.4f, 12f, 40, 8.0, ceCost);
    }

    private BeamConfig getHighConfig(UUID uuid) {
        int ceCost = (int) (plugin.ce().max(uuid) * 0.50);
        return new BeamConfig(0.8f, 20f, 80, 16.0, ceCost);
    }

    // ── LOW — small projectile ────────────────────────────────────────────────

    private void fireBlastProjectile(Player p, GraniteBlastSession session) {
        UUID uuid = p.getUniqueId();
        int ceCost = (int) (plugin.ce().max(uuid) * 0.10);
        if (!plugin.ce().tryConsume(uuid, ceCost)) {
            p.sendMessage(plugin.cfg().prefix() + "§cNot enough Cursed Energy.");
            blastSessions.remove(uuid);
            session.cleanup();
            return;
        }
        plugin.cooldowns().setCooldown(uuid, "ed.blast", 15);

        // Spawn small ItemDisplay projectile
        Location eyeLoc = p.getEyeLocation();
        ItemDisplay proj = (ItemDisplay) p.getWorld().spawnEntity(eyeLoc, EntityType.ITEM_DISPLAY);
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
                new Vector3f(0.25f, 0.25f, 0.75f),
                new Quaternionf()
        ));
        proj.setRotation(eyeLoc.getYaw(), eyeLoc.getPitch());
        session.beamEntity = proj;

        Vector dir = eyeLoc.getDirection().normalize();
        double[] pos = { eyeLoc.getX(), eyeLoc.getY(), eyeLoc.getZ() };
        double[] distTravelled = { 0.0 };
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

            // Max distance check
            if (distTravelled[0] >= maxDist) {
                proj.remove();
                session.cleanup();
                blastSessions.remove(uuid);
                return;
            }

            // Solid block collision
            if (newLoc.getBlock().getType().isSolid()) {
                proj.remove();
                session.cleanup();
                blastSessions.remove(uuid);
                return;
            }

            // Entity collision
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

    // ── MEDIUM / HIGH — sustained beam ───────────────────────────────────────

    private void fireBlastBeam(Player p, GraniteBlastSession session, BeamConfig cfg) {
        UUID uuid = p.getUniqueId();
        if (!plugin.ce().tryConsume(uuid, cfg.ceCost())) {
            p.sendMessage(plugin.cfg().prefix() + "§cNot enough Cursed Energy.");
            blastSessions.remove(uuid);
            session.cleanup();
            return;
        }

        long cooldown = cfg.durationTicks() <= 40 ? 30 : 60;
        plugin.cooldowns().setCooldown(uuid, "ed.blast", cooldown);

        // Spawn beam ItemDisplay
        Location eyeLoc = p.getEyeLocation();
        ItemDisplay beam = (ItemDisplay) p.getWorld().spawnEntity(eyeLoc, EntityType.ITEM_DISPLAY);
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
        beam.setRotation(eyeLoc.getYaw(), eyeLoc.getPitch());
        session.beamEntity = beam;

        int[] tickCount = { 0 };
        double halfWidth = cfg.thickness() / 2.0 + 0.5;
        Set<UUID> hitThisTick = new HashSet<>();
        double damagePerTick = cfg.damage() / 4.0;

        // Beam task: follow player aim, hit detection
        BukkitTask beamTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            tickCount[0]++;
            if (!p.isOnline() || !beam.isValid() || tickCount[0] > cfg.durationTicks()) {
                beam.remove();
                session.cleanup();
                blastSessions.remove(uuid);
                p.sendActionBar("§b⚡ Granite Blast complete.");
                return;
            }

            Location eye = p.getEyeLocation();
            beam.teleport(eye);
            beam.setRotation(eye.getYaw(), eye.getPitch());

            Vector beamDir = eye.getDirection().normalize();
            hitThisTick.clear();

            // Raycast hit detection along beam length
            for (double d = 0.5; d <= cfg.length(); d += 0.5) {
                Location checkLoc = eye.clone().add(beamDir.clone().multiply(d));
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

        // Spiral particle task around the beam
        double[] spiralAngle = { 0.0 };
        BukkitTask spiralTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!p.isOnline() || !beam.isValid()) return;

            Location eye = p.getEyeLocation();
            Vector beamDir = eye.getDirection().normalize();

            // Compute two perpendicular axes for the spiral
            Vector perp1 = beamDir.clone().crossProduct(new Vector(0, 1, 0)).normalize();
            if (perp1.lengthSquared() < 0.01) {
                perp1 = beamDir.clone().crossProduct(new Vector(1, 0, 0)).normalize();
            }
            Vector perp2 = beamDir.clone().crossProduct(perp1).normalize();

            double radius = 0.6;
            spiralAngle[0] += 0.5;

            for (double d = 0.0; d <= cfg.length(); d += 2.0) {
                double angle = spiralAngle[0] + d * 0.8;
                double ox = Math.cos(angle) * radius;
                double oy = Math.sin(angle) * radius;
                Location spiralLoc = eye.clone()
                        .add(beamDir.clone().multiply(d))
                        .add(perp1.clone().multiply(ox))
                        .add(perp2.clone().multiply(oy));
                spiralLoc.getWorld().spawnParticle(Particle.DUST, spiralLoc, 1, 0, 0, 0, 0, GRANITE_SPIRAL);
            }
        }, 0L, 1L);
        session.spiralTask = spiralTask;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // COMMAND-BASED ENTRY POINTS
    // ─────────────────────────────────────────────────────────────────────────

    /** Fires the tracking beam via /energydischarge tracking */
    public void cmdTracking(Player p) {
        startTrackingCharge(p);
    }

    /** Fires/charges the granite blast via /energydischarge blast (toggles start/release). */
    public void cmdBlast(Player p) {
        UUID uuid = p.getUniqueId();
        GraniteBlastSession session = blastSessions.get(uuid);
        if (session != null && session.isCharging()) {
            releaseBlastCharge(p);
        } else {
            startBlastCharge(p);
        }
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

    /** Returns the active blast session for a player, or null if none. */
    public GraniteBlastSession getBlastSession(UUID uuid) {
        return blastSessions.get(uuid);
    }

    /** Cleans up any active tasks for a player (e.g. on logout). */
    public void cleanup(UUID uuid) {
        cancelTrackingCharge(uuid);
        GraniteBlastSession session = blastSessions.remove(uuid);
        if (session != null) {
            session.cancel();
            session.cleanup();
        }
    }
}
