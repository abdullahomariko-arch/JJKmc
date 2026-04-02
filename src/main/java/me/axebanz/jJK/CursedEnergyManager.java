package me.axebanz.jJK;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public final class CursedEnergyManager {

    private final JJKCursedToolsPlugin plugin;
    private final PlayerDataStore store;
    private int taskId = -1;

    /** XP points required per displayed CE level */
    public static final int XP_PER_LEVEL = 25;

    public CursedEnergyManager(JJKCursedToolsPlugin plugin, PlayerDataStore store) {
        this.plugin = plugin;
        this.store = store;
    }

    // ===== CE Level Progression =====

    /** Returns the displayed CE level (0–100, or 0–200 for Six Eyes). */
    public int getCeLevel(UUID uuid) {
        return store.get(uuid).ceLevelXp / XP_PER_LEVEL;
    }

    /** Returns the max displayable CE level for this player. */
    public int getMaxCeLevel(UUID uuid) {
        if (plugin.sixEyes() != null) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && plugin.sixEyes().hasSixEyes(p)) return 200;
        }
        return 100;
    }

    /** Returns the raw XP stored for the player. */
    public int getCeLevelXp(UUID uuid) {
        return store.get(uuid).ceLevelXp;
    }

    /** Returns the max XP for this player (getMaxCeLevel * XP_PER_LEVEL). */
    public int getMaxCeLevelXp(UUID uuid) {
        return getMaxCeLevel(uuid) * XP_PER_LEVEL;
    }

    /**
     * Adds XP toward CE level progression and fires unlock notifications.
     * @param uuid player UUID
     * @param xp   amount of XP to add (mob kill ≈ 1, player kill ≈ 15)
     */
    public void addCeLevelXp(UUID uuid, int xp) {
        if (xp <= 0) return;
        PlayerProfile prof = store.get(uuid);
        int maxXp = getMaxCeLevelXp(uuid);
        int oldLevel = prof.ceLevelXp / XP_PER_LEVEL;
        prof.ceLevelXp = Math.min(maxXp, prof.ceLevelXp + xp);
        int newLevel = prof.ceLevelXp / XP_PER_LEVEL;
        store.save(uuid);

        if (newLevel > oldLevel) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                checkUnlocks(p, oldLevel, newLevel);
            }
        }
    }

    /** Forcefully sets the CE level XP. Used by admin commands. */
    public void setCeLevelXp(UUID uuid, int xp) {
        PlayerProfile prof = store.get(uuid);
        int maxXp = getMaxCeLevelXp(uuid);
        int oldLevel = prof.ceLevelXp / XP_PER_LEVEL;
        prof.ceLevelXp = Math.max(0, Math.min(maxXp, xp));
        int newLevel = prof.ceLevelXp / XP_PER_LEVEL;
        store.save(uuid);

        Player p = Bukkit.getPlayer(uuid);
        if (p != null && newLevel > oldLevel) {
            checkUnlocks(p, oldLevel, newLevel);
        }
    }

    /** Checks whether a CE level crosses an unlock threshold and notifies the player. */
    private void checkUnlocks(Player p, int oldLevel, int newLevel) {
        // Level 20 — Lower Fall Damage + Binding Vow
        if (oldLevel < 20 && newLevel >= 20) {
            p.sendMessage(plugin.cfg().prefix() + "§aCE Level 20 reached! §7Fall damage reduced by 40%.");
            p.sendMessage(plugin.cfg().prefix() + "§aBinding Vow selection is now available.");
        }
        // Level 50 — Black Flash (placeholder)
        if (oldLevel < 50 && newLevel >= 50) {
            p.sendMessage("§6You have unlocked Black Flash!");
        }
        // Level 100 — Reverse Cursed Technique
        if (oldLevel < 100 && newLevel >= 100) {
            int maxLevel = getMaxCeLevel(p.getUniqueId());
            // Limitless users need 200 to unlock RCT for Red ability
            String techId = plugin.techniqueManager().getAssignedId(p.getUniqueId());
            if ("limitless".equalsIgnoreCase(techId) && maxLevel == 200) {
                p.sendMessage(plugin.cfg().prefix() + "§aCE Level 100. §7Limitless users need level 200 to unlock RCT.");
            } else {
                p.sendMessage(plugin.cfg().prefix() + "§aReverse Cursed Technique unlocked! §7Crouch when below 3 hearts to heal.");
            }
        }
        // Level 200 — RCT for Limitless/Six Eyes
        if (oldLevel < 200 && newLevel >= 200) {
            p.sendMessage(plugin.cfg().prefix() + "§aReverse Cursed Technique is ready");
            p.sendMessage(plugin.cfg().prefix() + "§bLimitless §ared ability is now unlocked!");
        }
        // Notify CE level up
        p.sendMessage(plugin.cfg().prefix() + "§eCE Level Up! §7Now at §f" + newLevel + "/" + getMaxCeLevel(p.getUniqueId()));
    }

    /** Returns true if the player has unlocked Reverse Cursed Technique. */
    public boolean hasRct(UUID uuid) {
        int level = getCeLevel(uuid);
        String techId = plugin.techniqueManager().getAssignedId(uuid);
        // Limitless users need 200 levels for RCT
        if ("limitless".equalsIgnoreCase(techId)) {
            return level >= 200;
        }
        // Copy (Yuta) users with ring get RCT immediately
        Player p = Bukkit.getPlayer(uuid);
        if (p != null && plugin.copy() != null && plugin.copy().isCopyEquipped(p)) {
            return true;
        }
        return level >= 100;
    }

    /** Returns true if player has unlocked fall damage reduction (level >= 20). */
    public boolean hasFallDamageReduction(UUID uuid) {
        return getCeLevel(uuid) >= 20;
    }

    /**
     * Returns a slight damage resistance multiplier based on CE level (very gradual).
     * At level 100 gives 10% resistance; returns value to multiply damage by.
     */
    public double getDamageResistanceMultiplier(UUID uuid) {
        int level = getCeLevel(uuid);
        int maxLevel = getMaxCeLevel(uuid);
        // Scale from 0.0 to 0.10 resistance (0% to 10%) based on level
        double resistance = (level / (double) maxLevel) * 0.10;
        return 1.0 - resistance;
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
        String techId = plugin.techniqueManager().getAssignedId(uuid);
        if (plugin.projectionManager() != null && "projection".equalsIgnoreCase(techId)) return 20;
        if ("energy_discharge".equalsIgnoreCase(techId)) return 200;
        // Six Eyes + Limitless: 100 CE with Six Eyes, 50 CE without
        if ("limitless".equalsIgnoreCase(techId) && plugin.sixEyes() != null) {
            Player p = plugin.getServer().getPlayer(uuid);
            if (p != null && plugin.sixEyes().hasSixEyes(p)) return 100;
            return 50; // Default for Limitless without Six Eyes (or offline)
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