package me.axebanz.jJK;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DomainManager {
    private final Map<UUID, DomainExpansion> activeDomains = new HashMap<>();
    private final JJKCursedToolsPlugin plugin;

    public DomainManager(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    public void openDomain(Player player, DomainExpansion domain) {
        UUID uuid = player.getUniqueId();
        if (activeDomains.containsKey(uuid)) {
            player.sendMessage(plugin.cfg().prefix() + "§cYou already have an active domain.");
            return;
        }
        domain.open(player);
        activeDomains.put(uuid, domain);
    }

    public void closeDomain(Player player) {
        UUID uuid = player.getUniqueId();
        DomainExpansion domain = activeDomains.remove(uuid);
        if (domain != null) {
            domain.close(player);
        }
    }

    public DomainExpansion getDomain(UUID uuid) {
        return activeDomains.get(uuid);
    }

    public boolean hasDomain(UUID uuid) {
        return activeDomains.containsKey(uuid);
    }

    public boolean isBarrierBlock(Location loc) {
        for (DomainExpansion domain : activeDomains.values()) {
            if (domain.getBarrierBlocks().contains(loc)) {
                return true;
            }
        }
        return false;
    }

    public Collection<DomainExpansion> getAll() {
        return activeDomains.values();
    }
}
