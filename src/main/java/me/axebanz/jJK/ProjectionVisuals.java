package me.axebanz.jJK;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class ProjectionVisuals {

    // Light blue-purple / periwinkle #8C82FF = RGB(140, 130, 255)
    private static final Color PERIWINKLE = Color.fromRGB(140, 130, 255);
    private static final Color PERIWINKLE_BRIGHT = Color.fromRGB(160, 150, 255);
    private static final Color VIOLATION_COLOR = Color.fromRGB(255, 30, 30);
    private static final Color LOCK_COLOR = Color.fromRGB(140, 130, 255);

    private static final Particle.DustOptions TRAIL_DUST =
            new Particle.DustOptions(PERIWINKLE, 0.9f);
    private static final Particle.DustOptions AFTERIMAGE_DUST =
            new Particle.DustOptions(PERIWINKLE_BRIGHT, 1.2f);
    private static final Particle.DustOptions VIOLATION_DUST =
            new Particle.DustOptions(VIOLATION_COLOR, 1.5f);
    private static final Particle.DustOptions FRAME_LOCK_DUST =
            new Particle.DustOptions(PERIWINKLE, 1.2f);
    private static final Particle.DustOptions LOCK_BEAM_DUST =
            new Particle.DustOptions(LOCK_COLOR, 0.8f);
    private static final Particle.DustOptions BREAKER_DUST =
            new Particle.DustOptions(PERIWINKLE_BRIGHT, 1.8f);

    private final JJKCursedToolsPlugin plugin;

    public ProjectionVisuals(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    /** Light blue-purple dust trail at player position every tick */
    public void spawnTrail(Player p) {
        Location loc = p.getLocation().clone().add(0, 1.0, 0);
        p.getWorld().spawnParticle(Particle.DUST, loc, 6, 0.2, 0.4, 0.2, 0, TRAIL_DUST);
    }

    /**
     * Particle-based player silhouette outline every 4 ticks during dash.
     * Draws head, torso, and legs as particle outlines.
     */
    public void spawnAfterimage(Player p) {
        Location base = p.getLocation().clone();
        World w = p.getWorld();

        // Head (y+1.5 to y+2.0)
        spawnCircle(w, base.clone().add(0, 1.75, 0), 0.25, AFTERIMAGE_DUST);
        // Torso (y+0.8 to y+1.4)
        spawnRect(w, base, 0.3, 0.6, 1.4, AFTERIMAGE_DUST);
        // Legs (y+0.0 to y+0.7)
        spawnRect(w, base, 0.2, 0.0, 0.7, AFTERIMAGE_DUST);
    }

    private void spawnCircle(World w, Location center, double radius, Particle.DustOptions dust) {
        int points = 8;
        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            double dx = Math.cos(angle) * radius;
            double dz = Math.sin(angle) * radius;
            w.spawnParticle(Particle.DUST, center.clone().add(dx, 0, dz), 1, 0, 0, 0, 0, dust);
        }
    }

    private void spawnRect(World w, Location base, double halfWidth, double yMin, double yMax, Particle.DustOptions dust) {
        double step = 0.25;
        // Left and right sides
        for (double y = yMin; y <= yMax; y += step) {
            w.spawnParticle(Particle.DUST, base.clone().add(-halfWidth, y, 0), 1, 0, 0, 0, 0, dust);
            w.spawnParticle(Particle.DUST, base.clone().add(halfWidth, y, 0), 1, 0, 0, 0, 0, dust);
        }
        // Top and bottom edges
        for (double x = -halfWidth; x <= halfWidth; x += step) {
            w.spawnParticle(Particle.DUST, base.clone().add(x, yMin, 0), 1, 0, 0, 0, 0, dust);
            w.spawnParticle(Particle.DUST, base.clone().add(x, yMax, 0), 1, 0, 0, 0, 0, dust);
        }
    }

    /** Violation red particles + sound */
    public void spawnViolation(Player p) {
        Location loc = p.getLocation().clone().add(0, 1.0, 0);
        World w = p.getWorld();
        w.spawnParticle(Particle.DUST, loc, 60, 0.4, 0.8, 0.4, 0, VIOLATION_DUST);
        w.spawnParticle(Particle.CRIT, loc, 25, 0.4, 0.6, 0.4, 0.05);
        w.playSound(p.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 0.8f);
        w.playSound(p.getLocation(), Sound.ENTITY_IRON_GOLEM_HURT, 0.6f, 0.6f);
    }

    /** Frame Lock periwinkle particles + sound */
    public void spawnFrameLock(Player target) {
        Location loc = target.getLocation().clone();
        World w = target.getWorld();

        w.spawnParticle(Particle.DUST, loc.clone().add(0, 1.0, 0), 40, 0.3, 0.8, 0.3, 0, FRAME_LOCK_DUST);
        w.playSound(loc, Sound.BLOCK_GLASS_BREAK, 0.8f, 1.6f);
        w.playSound(loc, Sound.ENTITY_IRON_GOLEM_ATTACK, 0.5f, 1.8f);

        spawnFrameOutline(w, loc);
    }

    /** Frame lock outline for any entity */
    public void spawnEntityFrameLock(Location loc) {
        World w = loc.getWorld();
        if (w == null) return;
        w.spawnParticle(Particle.DUST, loc.clone().add(0, 1.0, 0), 40, 0.3, 0.8, 0.3, 0, FRAME_LOCK_DUST);
        w.playSound(loc, Sound.BLOCK_GLASS_BREAK, 0.8f, 1.6f);
        spawnFrameOutline(w, loc);
    }

    private void spawnFrameOutline(World w, Location center) {
        double size = 0.75;
        double height = 2.0;
        double step = 0.15;

        for (double x = -size; x <= size; x += step) {
            w.spawnParticle(Particle.DUST, center.clone().add(x, 0.1, -size), 1, 0, 0, 0, 0, FRAME_LOCK_DUST);
            w.spawnParticle(Particle.DUST, center.clone().add(x, 0.1, size), 1, 0, 0, 0, 0, FRAME_LOCK_DUST);
            w.spawnParticle(Particle.DUST, center.clone().add(-size, 0.1, x), 1, 0, 0, 0, 0, FRAME_LOCK_DUST);
            w.spawnParticle(Particle.DUST, center.clone().add(size, 0.1, x), 1, 0, 0, 0, 0, FRAME_LOCK_DUST);
        }
        for (double x = -size; x <= size; x += step) {
            w.spawnParticle(Particle.DUST, center.clone().add(x, height, -size), 1, 0, 0, 0, 0, FRAME_LOCK_DUST);
            w.spawnParticle(Particle.DUST, center.clone().add(x, height, size), 1, 0, 0, 0, 0, FRAME_LOCK_DUST);
            w.spawnParticle(Particle.DUST, center.clone().add(-size, height, x), 1, 0, 0, 0, 0, FRAME_LOCK_DUST);
            w.spawnParticle(Particle.DUST, center.clone().add(size, height, x), 1, 0, 0, 0, 0, FRAME_LOCK_DUST);
        }
        for (double y = 0; y <= height; y += step) {
            w.spawnParticle(Particle.DUST, center.clone().add(-size, y, -size), 1, 0, 0, 0, 0, FRAME_LOCK_DUST);
            w.spawnParticle(Particle.DUST, center.clone().add(size, y, -size), 1, 0, 0, 0, 0, FRAME_LOCK_DUST);
            w.spawnParticle(Particle.DUST, center.clone().add(-size, y, size), 1, 0, 0, 0, 0, FRAME_LOCK_DUST);
            w.spawnParticle(Particle.DUST, center.clone().add(size, y, size), 1, 0, 0, 0, 0, FRAME_LOCK_DUST);
        }
    }

    /** Lock-on beam from player to target */
    public void spawnLockBeam(Player p, Location targetLoc) {
        Location from = p.getLocation().clone().add(0, 1.5, 0);
        Vector dir = targetLoc.clone().subtract(from).toVector();
        double dist = dir.length();
        if (dist < 0.1) return;
        dir.normalize();

        double step = 1.0;
        for (double d = 0; d < dist; d += step) {
            Location point = from.clone().add(dir.clone().multiply(d));
            p.getWorld().spawnParticle(Particle.DUST, point, 1, 0.1, 0.1, 0.1, 0, LOCK_BEAM_DUST);
        }
    }

    /** Breaker explosion burst (light blue-purple) */
    public void spawnBreakerExplosion(Player p) {
        Location loc = p.getLocation().clone().add(0, 1.0, 0);
        World w = p.getWorld();
        w.spawnParticle(Particle.DUST, loc, 100, 0.8, 0.8, 0.8, 0, BREAKER_DUST);
        w.spawnParticle(Particle.CRIT, loc, 30, 0.5, 0.5, 0.5, 0.3);
        w.spawnParticle(Particle.FLASH, loc, 1, 0, 0, 0, 0);
        w.playSound(loc, Sound.BLOCK_GLASS_BREAK, 1.5f, 0.8f);
        w.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.2f);
    }

    /** Step sound every tick during ACTIVE phase */
    public void playStepSound(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.3f, 1.8f);
    }

    /** Update actionbar during ACTIVE phase */
    public void updateActionbar(Player p, int stepIndex, int totalSteps, int stacks) {
        int filled = (int) Math.round(10.0 * stepIndex / totalSteps);
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            bar.append(i < filled ? "§5█" : "§8█");
        }
        p.sendActionBar("§5▸ PROJECTION §8|§f " + bar + " §7(F:" + stepIndex + "/" + totalSteps + ") §8|§5 Stack: " + stacks);
    }

    /** Actionbar during PROGRAMMING phase */
    public void updateProgrammingActionbar(Player p, int ticksRemaining) {
        p.sendActionBar("§5▸ PROJECTION §8|§e LOCKING ON... §7(" + ticksRemaining + " ticks)");
    }

    /** Actionbar during FROZEN_PENALTY */
    public void updatePenaltyActionbar(Player p, int ticksRemaining) {
        p.sendActionBar("§c✖ PROJECTION INTERRUPTED §8|§c FROZEN §7(" + ticksRemaining + "t)");
    }

    /** OVERHEAT message */
    public void showOverheat(Player p) {
        p.sendMessage(plugin.cfg().prefix() + "§6⚠ PROJECTION OVERHEAT! §7Cooldown: §f5s");
        p.sendActionBar("§6⚠ OVERHEAT — Projection cooling down...");
    }
}
