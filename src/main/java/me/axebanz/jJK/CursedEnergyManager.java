package me.axebanz.jJK;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public final class CursedEnergyManager {

    private final JJKCursedToolsPlugin plugin;
    private final PlayerDataStore store;
    private int taskId = -1;

    public CursedEnergyManager(JJKCursedToolsPlugin plugin, PlayerDataStore store) {
        this.plugin = plugin;
        this.store = store;
    }

    public int get(UUID uuid) {
        return store.get(uuid).ce;
    }

    public int max(UUID uuid) {
        if (plugin.copy() != null) {
            Player p = plugin.getServer().getPlayer(uuid);
            if (p != null && plugin.copy().isCopyEquipped(p)) {
                return plugin.copy().ceMaxFor(uuid);
            }
        }
        if (plugin.projectionManager() != null) {
            String techId = plugin.techniqueManager().getAssignedId(uuid);
            if ("projection".equalsIgnoreCase(techId)) return 20;
        }
        return plugin.cfg().ceMax();
    }

    public void ensureInitialized(UUID uuid) {
        PlayerProfile prof = store.get(uuid);
        int max = max(uuid);
        if (prof.ce <= 0) prof.ce = max;
        if (prof.ce > max) prof.ce = max;
        store.save(uuid);
    }

    public boolean tryConsume(UUID uuid, int amount) {
        if (amount <= 0) return true;
        PlayerProfile prof = store.get(uuid);
        int max = max(uuid);
        if (prof.ce > max) prof.ce = max;
        if (prof.ce < amount) return false;
        prof.ce -= amount;
        store.save(uuid);
        return true;
    }

    public void add(UUID uuid, int amount) {
        if (amount <= 0) return;
        PlayerProfile prof = store.get(uuid);
        int max = max(uuid);
        prof.ce = Math.min(max, prof.ce + amount);
        store.save(uuid);
    }

    public void startRegenTask() {
        if (taskId != -1) Bukkit.getScheduler().cancelTask(taskId);
        int seconds = plugin.cfg().ceRegenTickSeconds();
        int amt = plugin.cfg().ceRegenAmount();
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                UUID u = p.getUniqueId();
                int currentMax = max(u);
                PlayerProfile prof = store.get(u);

                // Clamp down if over max (technique changed)
                if (prof.ce > currentMax) {
                    prof.ce = currentMax;
                    store.save(u);
                }

                // Regen CE up to the current max
                if (prof.ce < currentMax) {
                    add(u, amt);
                }
            }
        }, 20L * seconds, 20L * seconds);
    }
}