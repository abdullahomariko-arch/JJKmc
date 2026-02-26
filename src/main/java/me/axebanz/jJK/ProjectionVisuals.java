package me.axebanz.jJK;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;

import java.util.List;

public class ProjectionVisuals {

    public void spawnDashParticle(Location loc) {
        World world = loc.getWorld();
        if (world == null) return;
        world.spawnParticle(Particle.ELECTRIC_SPARK, loc, 3, 0.1, 0.1, 0.1, 0.0);
    }

    public void spawnPathParticles(List<Location> path) {
        for (Location loc : path) {
            spawnDashParticle(loc);
        }
    }

    public void spawnBreakerParticle(Location loc) {
        World world = loc.getWorld();
        if (world == null) return;
        world.spawnParticle(Particle.CRIT, loc, 5, 0.2, 0.2, 0.2, 0.0);
    }

    public void spawnBreakerPathParticles(List<Location> path) {
        for (Location loc : path) {
            spawnBreakerParticle(loc);
        }
    }
}
