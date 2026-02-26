package me.axebanz.jJK;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CursedEnergyManager {
    private final Map<UUID, Integer> energy = new HashMap<>();
    private final ConfigManager cfg;

    public CursedEnergyManager(ConfigManager cfg) {
        this.cfg = cfg;
    }

    public int get(UUID uuid) {
        return energy.getOrDefault(uuid, cfg.ceMax());
    }

    public void set(UUID uuid, int amount) {
        energy.put(uuid, Math.max(0, Math.min(cfg.ceMax(), amount)));
    }

    public boolean consume(UUID uuid, int amount) {
        int current = get(uuid);
        if (current < amount) return false;
        set(uuid, current - amount);
        return true;
    }

    public void regen(UUID uuid) {
        int current = get(uuid);
        int max = cfg.ceMax();
        if (current < max) {
            set(uuid, Math.min(max, current + cfg.ceRegenRate()));
        }
    }

    public void reset(UUID uuid) {
        energy.put(uuid, cfg.ceMax());
    }

    public int getMax() {
        return cfg.ceMax();
    }
}
