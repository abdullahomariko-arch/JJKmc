package me.axebanz.jJK;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manages the Projection Sorcery technique mechanics.
 * Bug Fix #1: Instant teleport — the player is teleported to the final destination
 * immediately (not tick-by-tick). Path is computed for collision/freeze/visual checks.
 */
public class ProjectionManager {
    private final JJKCursedToolsPlugin plugin;
    private final Map<UUID, ProjectionPlayerData> playerData = new HashMap<>();
    private final ProjectionVisuals visuals;
    private final ProjectionFreezeHandler freezeHandler;
    private final CooldownManager cooldowns;

    private static final double STEP_SIZE = 0.5;

    public ProjectionManager(JJKCursedToolsPlugin plugin, ProjectionFreezeHandler freezeHandler,
                             CooldownManager cooldowns) {
        this.plugin = plugin;
        this.visuals = new ProjectionVisuals();
        this.freezeHandler = freezeHandler;
        this.cooldowns = cooldowns;
    }

    public ProjectionPlayerData getData(UUID uuid) {
        return playerData.computeIfAbsent(uuid, ProjectionPlayerData::new);
    }

    // Called when player starts programming (looks in direction)
    public void startProgramming(Player player) {
        ProjectionPlayerData data = getData(player.getUniqueId());
        if (data.getState() != ProjectionState.IDLE) return;
        data.setState(ProjectionState.PROGRAMMING);
        data.setProgrammingStart(player.getLocation().clone());
        data.setProgrammedDirection(player.getLocation().getDirection().normalize());
        player.sendMessage(plugin.cfg().prefix() + "§bProgramming path...");
    }

    // Called when player commits path (e.g., right-click to end programming)
    // Bug Fix #1: instantly teleport to final destination (not tick-by-tick)
    public void commitProgramming(Player player, double distance) {
        ProjectionPlayerData data = getData(player.getUniqueId());
        if (data.getState() != ProjectionState.PROGRAMMING) return;

        data.setProgrammedDistance(distance);
        data.setState(ProjectionState.ACTIVE);

        Vector dir = data.getProgrammedDirection();
        Location start = player.getLocation().clone();

        // Compute the full path
        List<Location> path = buildPath(start, dir, distance);
        data.setPath(path);

        // Find final position (last non-colliding step)
        Location finalPos = getFinalPosition(start, path, player);

        // Spawn visuals along the path
        visuals.spawnPathParticles(path.subList(0, Math.min(path.size(),
                path.indexOf(finalPos) < 0 ? path.size() : path.indexOf(finalPos) + 1)));

        // Instant teleport to final position (Bug Fix #1)
        if (finalPos != null && !finalPos.equals(start)) {
            finalPos.setYaw(player.getLocation().getYaw());
            finalPos.setPitch(player.getLocation().getPitch());
            player.teleport(finalPos);
        }

        onActiveComplete(player, data, finalPos != null && !finalPos.equals(start));
    }

    private List<Location> buildPath(Location start, Vector dir, double distance) {
        List<Location> path = new ArrayList<>();
        double traveled = 0.0;
        Location current = start.clone();
        while (traveled < distance) {
            double step = Math.min(STEP_SIZE, distance - traveled);
            Vector stepVec = dir.clone().multiply(step);
            current = current.clone().add(stepVec);
            path.add(current.clone());
            traveled += step;
        }
        return path;
    }

    private Location getFinalPosition(Location start, List<Location> path, Player player) {
        Location last = start;
        for (Location loc : path) {
            if (isBlocked(loc)) {
                break;
            }
            last = loc;
        }
        return last;
    }

    private boolean isBlocked(Location loc) {
        if (loc.getWorld() == null) return true;
        Material type = loc.getBlock().getType();
        return type.isSolid() && !type.isAir();
    }

    private void onActiveComplete(Player player, ProjectionPlayerData data, boolean moved) {
        // After dash — freeze the player briefly
        int freezeTime = plugin.cfg().projectionFreezeDuration();
        freezeHandler.freeze(player, freezeTime);
        data.setState(ProjectionState.FROZEN);
        player.sendMessage(plugin.cfg().prefix() + "§bProjection complete.");
    }

    // Called when player uses Divergent Fist Breaker — lunge forward instantly
    // Bug Fix #1: BREAKER_LUNGE is instant teleport
    public void activateBreakerLunge(Player player) {
        ProjectionPlayerData data = getData(player.getUniqueId());
        if (data.getState() == ProjectionState.IDLE || data.getState() == ProjectionState.PROGRAMMING) return;

        data.setBreakerOrigin(player.getLocation().clone());
        data.setState(ProjectionState.BREAKER_LUNGE);

        Vector dir = player.getLocation().getDirection().normalize();
        double distance = plugin.cfg().projectionDashDistance() / 2.0;
        List<Location> path = buildPath(player.getLocation(), dir, distance);

        Location finalPos = getFinalPosition(player.getLocation(), path, player);

        // Spawn breaker visuals
        visuals.spawnBreakerPathParticles(path);

        // Instant teleport (Bug Fix #1)
        if (finalPos != null && !finalPos.equals(player.getLocation())) {
            finalPos.setYaw(player.getLocation().getYaw());
            finalPos.setPitch(player.getLocation().getPitch());
            player.teleport(finalPos);
        }

        player.sendMessage(plugin.cfg().prefix() + "§cBreaker Lunge!");
        data.setState(ProjectionState.IDLE);
    }

    // Bug Fix #1: BREAKER_BACK is instant teleport
    public void activateBreakerBack(Player player) {
        ProjectionPlayerData data = getData(player.getUniqueId());
        if (data.getState() == ProjectionState.IDLE) return;

        Location origin = data.getBreakerOrigin();
        if (origin == null) {
            data.setState(ProjectionState.IDLE);
            return;
        }

        data.setState(ProjectionState.BREAKER_BACK);

        // Compute path back to origin
        Vector dir = origin.toVector().subtract(player.getLocation().toVector()).normalize();
        double distance = player.getLocation().distance(origin);
        List<Location> path = buildPath(player.getLocation(), dir, distance);

        // Spawn breaker back visuals
        visuals.spawnBreakerPathParticles(path);

        // Instant teleport back (Bug Fix #1)
        origin.setYaw(player.getLocation().getYaw());
        origin.setPitch(player.getLocation().getPitch());
        player.teleport(origin);

        player.sendMessage(plugin.cfg().prefix() + "§cBreaker Back!");
        data.setBreakerOrigin(null);
        data.setState(ProjectionState.IDLE);
    }

    public void unfreezePlayer(Player player) {
        ProjectionPlayerData data = getData(player.getUniqueId());
        if (data.getState() == ProjectionState.FROZEN) {
            data.setState(ProjectionState.IDLE);
            freezeHandler.unfreeze(player);
        }
    }

    public boolean isFrozen(UUID uuid) {
        return freezeHandler.isFrozen(uuid);
    }

    public void onPlayerQuit(UUID uuid) {
        playerData.remove(uuid);
    }
}
