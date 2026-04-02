package me.axebanz.jJK;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class DomainExpansion {

    protected final JJKCursedToolsPlugin plugin;
    protected final Player caster;
    protected final Location center;
    protected final Map<Location, BlockState> savedBlocks = new HashMap<>();
    protected final Set<Location> barrierBlocks = new HashSet<>();
    protected boolean active = false;
    protected boolean fullyExpanded = false;
    protected BukkitTask tickTask = null;
    protected double refinement = 5.0;

    protected DomainExpansion(JJKCursedToolsPlugin plugin, Player caster) {
        this.plugin = plugin;
        this.caster = caster;
        this.center = caster.getLocation().clone();
    }

    public abstract String getName();
    public abstract void buildInterior();
    public abstract void onSureHit(Player target);
    public abstract void onTick();
    public abstract void onDomainEnd();
    public abstract int getRadius();
    public double getRefinement() { return refinement; }

    /**
     * Returns the effective radius accounting for CE level scaling.
     * Formula: baseRadius + (ceLevel / 10)
     * At CE level 50 the domain is 5 blocks bigger than base.
     */
    public int getEffectiveRadius() {
        int ceLevel = plugin.ce().getCeLevel(caster.getUniqueId());
        return getRadius() + (ceLevel / 10);
    }

    /**
     * How many ticks between each expansion step.
     * Override in subclasses for faster/slower growth.
     * Default: 3 ticks per shell layer (~0.15s per layer).
     */
    protected int getExpansionTickDelay() {
        return 3;
    }

    public void expand() {
        if (active) return;
        active = true;
        fullyExpanded = false;

        int totalRadius = getEffectiveRadius();
        int tickDelay = getExpansionTickDelay();

        // Phase 1: Animate the barrier growing outward shell by shell
        for (int r = 1; r <= totalRadius; r++) {
            final int currentRadius = r;
            final boolean isLastShell = (r == totalRadius);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!active) return;

                // Build barrier shell at this radius
                applyBarrierShell(currentRadius);

                // Spawn expansion particles at the edge
                spawnExpansionRing(currentRadius);

                // Play growth sound
                World w = center.getWorld();
                if (w != null) {
                    float pitch = 0.6f + (currentRadius / (float) totalRadius) * 1.2f;
                    w.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.8f, pitch);
                }

                // On final shell: build interior and start the tick loop
                if (isLastShell) {
                    buildInterior();
                    fullyExpanded = true;

                    World world = center.getWorld();
                    if (world != null) {
                        world.playSound(center, Sound.BLOCK_END_PORTAL_SPAWN, 0.7f, 1.2f);
                    }

                    caster.sendMessage(plugin.cfg().prefix() + "§aDomain fully expanded!");

                    // Start the sure-hit tick loop
                    tickTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
                        if (!active) {
                            tickTask.cancel();
                            return;
                        }
                        onTick();
                        for (Player p : getPlayersInside()) {
                            if (!p.equals(caster)) {
                                onSureHit(p);
                            }
                        }
                    }, 20L, 20L);
                }
            }, (long) r * tickDelay);
        }
    }

    public void collapse() {
        if (!active) return;
        active = false;
        fullyExpanded = false;

        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }

        // Restore all saved blocks
        for (Map.Entry<Location, BlockState> entry : savedBlocks.entrySet()) {
            Block block = entry.getKey().getBlock();
            block.setBlockData(entry.getValue().getBlockData(), false);
        }
        savedBlocks.clear();
        barrierBlocks.clear();

        onDomainEnd();
    }

    public boolean isInside(Location loc) {
        if (loc.getWorld() == null || center.getWorld() == null) return false;
        if (!loc.getWorld().equals(center.getWorld())) return false;
        return loc.distanceSquared(center) <= (double) getEffectiveRadius() * getEffectiveRadius();
    }

    public Set<Player> getPlayersInside() {
        Set<Player> inside = new HashSet<>();
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (isInside(p.getLocation())) {
                inside.add(p);
            }
        }
        return inside;
    }

    /**
     * Builds ONE shell of barrier at the given radius.
     * Called incrementally during the animated expansion.
     */
    private void applyBarrierShell(int r) {
        World w = center.getWorld();
        if (w == null) return;

        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();

        for (int x = cx - r; x <= cx + r; x++) {
            for (int y = cy - r; y <= cy + r; y++) {
                for (int z = cz - r; z <= cz + r; z++) {
                    double dist = Math.sqrt((x - cx) * (x - cx) + (y - cy) * (y - cy) + (z - cz) * (z - cz));
                    if (dist >= r - 1.0 && dist <= r) {
                        Location loc = new Location(w, x, y, z);
                        Block block = loc.getBlock();

                        // Only save/set if not already placed by a previous shell
                        if (!savedBlocks.containsKey(loc)) {
                            savedBlocks.put(loc, block.getState());
                        }
                        block.setType(Material.BARRIER, false);
                        barrierBlocks.add(loc);
                    }
                }
            }
        }

        // Remove previous shell's barrier blocks (only keep the outermost shell as barrier)
        // This makes the "wall" move outward instead of filling the whole sphere
        if (r > 1) {
            int prevR = r - 1;
            Set<Location> toRemove = new HashSet<>();
            for (Location loc : barrierBlocks) {
                double dist = Math.sqrt(
                        (loc.getBlockX() - cx) * (loc.getBlockX() - cx) +
                                (loc.getBlockY() - cy) * (loc.getBlockY() - cy) +
                                (loc.getBlockZ() - cz) * (loc.getBlockZ() - cz)
                );
                // If this block was part of a PREVIOUS shell (interior), clear it back to air
                // so only the outermost shell remains as barrier
                if (dist < r - 1.0) {
                    toRemove.add(loc);
                }
            }
            for (Location loc : toRemove) {
                Block block = loc.getBlock();
                // Restore to original or air
                BlockState original = savedBlocks.get(loc);
                if (original != null) {
                    block.setBlockData(original.getBlockData(), false);
                } else {
                    block.setType(Material.AIR, false);
                }
                barrierBlocks.remove(loc);
            }
        }
    }

    /**
     * Spawns a horizontal particle ring at the current expansion edge.
     */
    private void spawnExpansionRing(int r) {
        World w = center.getWorld();
        if (w == null) return;

        Location ringCenter = center.clone().add(0, 0.5, 0);
        int points = Math.max(16, r * 6);

        Particle.DustOptions dust = new Particle.DustOptions(
                org.bukkit.Color.fromRGB(180, 120, 255), 1.5f
        );

        for (int i = 0; i < points; i++) {
            double angle = (Math.PI * 2.0) * (i / (double) points);
            double x = Math.cos(angle) * r;
            double z = Math.sin(angle) * r;
            Location point = ringCenter.clone().add(x, 0, z);
            w.spawnParticle(Particle.DUST, point, 2, 0.1, 0.3, 0.1, 0, dust);
            w.spawnParticle(Particle.END_ROD, point, 1, 0.05, 0.2, 0.05, 0.01);
        }
    }

    /**
     * Legacy method — called by subclasses that override applyBarrier().
     * For the base class, the animated expansion handles barrier creation.
     */
    public void applyBarrier() {
        // No-op in base class — animated expansion handles this.
        // Subclasses (like IdleDeathGambleDomain) can override this for custom barrier materials.
    }

    public boolean isBarrierBlock(Location loc) {
        Location blockLoc = new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        return barrierBlocks.contains(blockLoc);
    }

    /**
     * Returns true if this block is ANYWHERE inside the domain.
     * Used to prevent breaking ANY block inside the domain.
     */
    public boolean isProtectedBlock(Location loc) {
        return isInside(loc);
    }

    public boolean isActive() { return active; }
    public boolean isFullyExpanded() { return fullyExpanded; }
    public Player getCaster() { return caster; }
    public Location getCenter() { return center; }
}