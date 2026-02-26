package me.axebanz.jJK;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class NullifyManager {
    private final Set<UUID> nullified = new HashSet<>();

    public void nullify(UUID uuid) {
        nullified.add(uuid);
    }

    public void restore(UUID uuid) {
        nullified.remove(uuid);
    }

    public boolean isNullified(UUID uuid) {
        return nullified.contains(uuid);
    }
}
