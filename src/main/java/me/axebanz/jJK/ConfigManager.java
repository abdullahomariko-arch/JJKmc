package me.axebanz.jJK;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class ConfigManager {
    private final JavaPlugin plugin;
    private FileConfiguration cfg;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        cfg = plugin.getConfig();
    }

    public String prefix() {
        return cfg.getString("prefix", "§5§l[JJK]§r ");
    }

    public int ceMax() {
        return cfg.getInt("cursed-energy.max", 100);
    }

    public int ceRegenRate() {
        return cfg.getInt("cursed-energy.regen-rate", 2);
    }

    public int ceRegenInterval() {
        return cfg.getInt("cursed-energy.regen-interval", 20);
    }

    public double projectionDashDistance() {
        return cfg.getDouble("projection.dash-distance", 10.0);
    }

    public long projectionDashCooldown() {
        return cfg.getLong("projection.dash-cooldown", 5000L);
    }

    public int projectionFreezeDuration() {
        return cfg.getInt("projection.freeze-duration", 60);
    }

    public long projectionBreakerCooldown() {
        return cfg.getLong("projection.breaker-cooldown", 8000L);
    }

    public int domainRadius() {
        return cfg.getInt("domain.radius", 15);
    }

    public int domainDuration() {
        return cfg.getInt("domain.duration", 300);
    }

    public int domainCeDrain() {
        return cfg.getInt("domain.ce-drain", 5);
    }

    public int seanceIncantationTime() {
        return cfg.getInt("seance.incantation-time", 100);
    }

    public boolean permadeathEnabled() {
        return cfg.getBoolean("permadeath.enabled", true);
    }

    public double cursedBodyDropChance() {
        return cfg.getDouble("cursed-body.drop-chance", 0.05);
    }

    public FileConfiguration raw() {
        return cfg;
    }
}
