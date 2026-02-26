package me.axebanz.jJK;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class TechniqueRegistry {
    private final Map<String, Technique> registry = new HashMap<>();

    public void register(Technique technique) {
        registry.put(technique.getId().toLowerCase(), technique);
    }

    public Technique get(String id) {
        if (id == null) return null;
        return registry.get(id.toLowerCase());
    }

    public Collection<Technique> all() {
        return registry.values();
    }

    public boolean has(String id) {
        return id != null && registry.containsKey(id.toLowerCase());
    }
}
