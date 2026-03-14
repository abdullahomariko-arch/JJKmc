package me.axebanz.jJK;

import java.util.*;

public final class TechniqueRegistry {

    private final Map<String, Technique> techniques = new HashMap<>();

    public void register(Technique t) {
        techniques.put(t.id().toLowerCase(Locale.ROOT), t);
    }

    public Technique get(String id) {
        if (id == null) return null;
        return techniques.get(id.toLowerCase(Locale.ROOT));
    }

    public Set<String> ids() {
        return Collections.unmodifiableSet(techniques.keySet());
    }
}
