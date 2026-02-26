package me.axebanz.jJK;

import org.bukkit.Location;

public class PhenomenonDetector {
    private final JJKCursedToolsPlugin plugin;

    public PhenomenonDetector(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    public PhenomenonType detect(Location loc) {
        // Check for phenomena at given location
        if (plugin.domainManager().getAll().stream()
                .anyMatch(d -> d.getCenter().distanceSquared(loc) <= d.getRadius() * d.getRadius())) {
            return PhenomenonType.DOMAIN_OVERLAP;
        }
        return null;
    }
}
