package me.axebanz.jJK;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DomainManager {

    private final JJKCursedToolsPlugin plugin;
    private final Map<UUID, DomainExpansion> activeDomains = new ConcurrentHashMap<>();

    public DomainManager(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    public void expand(Player caster, DomainExpansion domain) {
        UUID u = caster.getUniqueId();

        // Collapse existing domain if any
        DomainExpansion existing = activeDomains.get(u);
        if (existing != null) {
            existing.collapse();
        }

        // Check for domain clash
        for (Map.Entry<UUID, DomainExpansion> entry : activeDomains.entrySet()) {
            DomainExpansion other = entry.getValue();
            if (other.isInside(caster.getLocation()) || domain.isInside(other.getCaster().getLocation())) {
                // Domain clash: higher refinement wins
                if (domain.getRefinement() > other.getRefinement()) {
                    other.collapse();
                    activeDomains.remove(entry.getKey());
                    Player otherCaster = other.getCaster();
                    otherCaster.sendMessage(plugin.cfg().prefix() + "§cYour Domain was crushed by a stronger expansion!");
                } else {
                    caster.sendMessage(plugin.cfg().prefix() + "§cA stronger Domain is already active here!");
                    return;
                }
            }
        }

        activeDomains.put(u, domain);
        domain.expand();
    }

    public void collapse(Player caster) {
        UUID u = caster.getUniqueId();
        DomainExpansion domain = activeDomains.remove(u);
        if (domain != null) {
            domain.collapse();
        }
    }

    public DomainExpansion getDomain(Player caster) {
        return activeDomains.get(caster.getUniqueId());
    }

    public boolean isInsideAnyDomain(Location loc) {
        for (DomainExpansion d : activeDomains.values()) {
            if (d.isInside(loc)) return true;
        }
        return false;
    }

    public DomainExpansion getDomainAt(Location loc) {
        for (DomainExpansion d : activeDomains.values()) {
            if (d.isInside(loc)) return d;
        }
        return null;
    }

    public Collection<DomainExpansion> getActiveDomains() {
        return activeDomains.values();
    }

    public void collapseAll() {
        for (DomainExpansion d : activeDomains.values()) {
            d.collapse();
        }
        activeDomains.clear();
    }
}
