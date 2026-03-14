package me.axebanz.jJK;

import org.bukkit.configuration.file.FileConfiguration;

public final class ConfigManager {

    private final JJKCursedToolsPlugin plugin;
    private FileConfiguration config;

    public ConfigManager(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    public FileConfiguration c() {
        return config;
    }

    public String prefix() {
        return c().getString("prefix", "§8[§dJJK§8]§r ");
    }

    public boolean persistCooldownsOnDeath() {
        return c().getBoolean("data.persistCooldownsOnDeath", true);
    }

    public int ceMax() {
        return Math.max(1, c().getInt("cursedEnergy.max", 10));
    }

    public int ceRegenAmount() {
        return Math.max(1, c().getInt("cursedEnergy.regen.amountPerTick", 1));
    }

    public int ceRegenTickSeconds() {
        return Math.max(1, c().getInt("cursedEnergy.regen.tickSeconds", 4));
    }

    public boolean ceBossbarEnabled() {
        return c().getBoolean("ui.cursedEnergyBossbar.enabled", true);
    }

    public int cooldownBossbarThreshold() {
        return Math.max(1, c().getInt("ui.cooldownBossbar.longTimerThresholdSeconds", 45));
    }

    public boolean cooldownActionbarEnabled() {
        return c().getBoolean("ui.cooldownActionbar.enabled", true);
    }

    public boolean cooldownPreferShortest() {
        return c().getBoolean("ui.cooldownActionbar.preferShortest", true);
    }

    public boolean permadeathEnabled() {
        return c().getBoolean("permadeath.enabled", false);
    }

    public void setPermadeathEnabled(boolean enabled) {
        c().set("permadeath.enabled", enabled);
        plugin.saveConfig();
    }
}
