package me.axebanz.jJK;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Base class for Domain Expansion.
 * Bug Fix #5: getBarrierMaterial() returns BLACK_CONCRETE by default.
 * applyBarrier() uses getBarrierMaterial().
 */
public abstract class DomainExpansion {
    protected final JJKCursedToolsPlugin plugin;
    protected final UUID ownerUuid;
    protected final Location center;
    protected final int radius;
    protected final List<Location> barrierBlocks = new ArrayList<>();
    protected boolean active = false;

    public DomainExpansion(JJKCursedToolsPlugin plugin, UUID ownerUuid, Location center) {
        this.plugin = plugin;
        this.ownerUuid = ownerUuid;
        this.center = center.clone();
        this.radius = plugin.cfg().domainRadius();
    }

    public abstract String getId();
    public abstract String getDisplayName();

    // Bug Fix #5: returns BLACK_CONCRETE by default — subclasses can override
    protected Material getBarrierMaterial() {
        return Material.BLACK_CONCRETE;
    }

    public void open(Player player) {
        active = true;
        applyBarrier();
        onOpen(player);
    }

    public void close(Player player) {
        removeBarrier();
        active = false;
        onClose(player);
    }

    // Bug Fix #5: uses getBarrierMaterial() instead of hardcoded material
    protected void applyBarrier() {
        World world = center.getWorld();
        if (world == null) return;
        Material mat = getBarrierMaterial();
        int r = radius;
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    double dist = Math.sqrt(x * x + y * y + z * z);
                    if (dist >= r - 1 && dist <= r) {
                        Location loc = center.clone().add(x, y, z);
                        if (loc.getBlock().getType().isAir()) {
                            loc.getBlock().setType(mat);
                            barrierBlocks.add(loc);
                        }
                    }
                }
            }
        }
    }

    protected void removeBarrier() {
        for (Location loc : barrierBlocks) {
            if (loc.getBlock().getType() == getBarrierMaterial()) {
                loc.getBlock().setType(Material.AIR);
            }
        }
        barrierBlocks.clear();
    }

    protected abstract void onOpen(Player player);
    protected abstract void onClose(Player player);

    public boolean isActive() { return active; }
    public UUID getOwnerUuid() { return ownerUuid; }
    public Location getCenter() { return center; }
    public int getRadius() { return radius; }
    public List<Location> getBarrierBlocks() { return barrierBlocks; }
}
