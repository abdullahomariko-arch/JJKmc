package me.axebanz.jJK;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TechniqueManager {
    private final Map<UUID, String> assigned = new HashMap<>();
    private final TechniqueRegistry registry;

    public TechniqueManager(TechniqueRegistry registry) {
        this.registry = registry;
    }

    public String getAssignedId(UUID uuid) {
        return assigned.get(uuid);
    }

    public Technique getAssigned(UUID uuid) {
        String id = assigned.get(uuid);
        if (id == null) return null;
        return registry.get(id);
    }

    public void assign(UUID uuid, String techniqueId) {
        assigned.put(uuid, techniqueId == null ? null : techniqueId.toLowerCase());
    }

    public void unassign(UUID uuid) {
        assigned.remove(uuid);
    }

    public boolean hasAssigned(UUID uuid) {
        return assigned.containsKey(uuid) && assigned.get(uuid) != null;
    }

    public boolean hasAssigned(UUID uuid, String techniqueId) {
        String current = assigned.get(uuid);
        return current != null && current.equalsIgnoreCase(techniqueId);
    }

    public void castAbility(Player player, String ability) {
        Technique t = getAssigned(player.getUniqueId());
        if (t != null) {
            t.castAbility(player, ability);
        }
    }
}
