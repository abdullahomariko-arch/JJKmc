package me.axebanz.jJK;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles CE level progression events:
 *  - Kill tracking (mob/player kills grant CE XP)
 *  - Fall damage reduction at level 20
 *  - Reverse Cursed Technique (RCT) at level 100 / crouch
 *  - Gradual damage resistance scaling
 */
public final class CursedEnergyListener implements Listener {

    private final JJKCursedToolsPlugin plugin;

    /** Tracks players currently in RCT healing (to avoid re-triggering) */
    private final Map<UUID, Long> rctActiveUntilMs = new ConcurrentHashMap<>();

    /** Mob kill XP — each regular mob kill */
    private static final int MOB_KILL_XP = 5;
    /** Cursed mob kill XP — harder mobs give more XP */
    private static final int CURSED_MOB_KILL_XP = 15;
    /** Player kill XP */
    private static final int PLAYER_KILL_XP = 15;

    /** RCT heal duration in seconds */
    private static final int RCT_DURATION_SECONDS = 5;
    /** RCT cooldown in seconds */
    private static final int RCT_COOLDOWN_SECONDS = 60;
    /** RCT heal per tick (0.5 hearts = 1 HP per 10 ticks) */
    private static final double RCT_HEAL_PER_TICK = 1.0;

    public CursedEnergyListener(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    // ===== Kill tracking for CE XP =====

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent e) {
        Player killer = e.getEntity().getKiller();
        if (killer == null) return;

        UUID killerUuid = killer.getUniqueId();
        int xp;
        if (e.getEntity() instanceof Player) {
            xp = PLAYER_KILL_XP;
        } else if (plugin.cursedMobs() != null && plugin.cursedMobs().isCursedMob(e.getEntity())) {
            xp = CURSED_MOB_KILL_XP;
        } else {
            xp = MOB_KILL_XP;
        }

        plugin.ce().addCeLevelXp(killerUuid, xp);

        // Show XP gain to player
        killer.sendActionBar(plugin.cfg().prefix() + "§a+" + xp + " CE XP");
    }

    // ===== Fall damage reduction at CE level >= 20 =====

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFallDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (e.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        if (!plugin.ce().hasFallDamageReduction(p.getUniqueId())) return;

        // 40% fall damage reduction
        e.setDamage(e.getDamage() * 0.60);
    }

    // ===== Gradual damage resistance from CE level =====

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onGenericDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (e.getCause() == EntityDamageEvent.DamageCause.FALL) return; // handled above

        double multiplier = plugin.ce().getDamageResistanceMultiplier(p.getUniqueId());
        if (multiplier < 1.0) {
            e.setDamage(e.getDamage() * multiplier);
        }
    }

    // ===== RCT crouch detection =====

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent e) {
        if (!e.isSneaking()) return;

        Player p = e.getPlayer();
        UUID uuid = p.getUniqueId();

        // Must have RCT unlocked
        if (!plugin.ce().hasRct(uuid)) return;

        // Must be below 3 hearts (6 HP)
        if (p.getHealth() >= 6.0) return;

        // Check cooldown
        long now = System.currentTimeMillis();
        if (plugin.data().get(uuid).rctCooldownUntilMs > now) {
            long remSec = (plugin.data().get(uuid).rctCooldownUntilMs - now) / 1000;
            p.sendMessage(plugin.cfg().prefix() + "§cRCT is on cooldown: §f" + remSec + "s");
            return;
        }

        // Not already healing
        if (rctActiveUntilMs.getOrDefault(uuid, 0L) > now) return;

        activateRct(p);
    }

    private void activateRct(Player p) {
        UUID uuid = p.getUniqueId();
        long now = System.currentTimeMillis();
        long until = now + RCT_DURATION_SECONDS * 1000L;

        rctActiveUntilMs.put(uuid, until);

        // Set cooldown
        PlayerProfile prof = plugin.data().get(uuid);
        prof.rctCooldownUntilMs = now + RCT_COOLDOWN_SECONDS * 1000L;
        plugin.data().save(uuid);

        p.sendMessage(plugin.cfg().prefix() + "§aReverse Cursed Technique §7activated!");

        // Make player invisible-ish (no actual invisibility to avoid potion effect showing)
        // Apply regeneration effect for 5 seconds
        p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, RCT_DURATION_SECONDS * 20, 4, false, false, true));

        // Rapid heal task
        int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!p.isOnline() || rctActiveUntilMs.getOrDefault(uuid, 0L) <= System.currentTimeMillis()) return;
            double newHp = Math.min(p.getMaxHealth(), p.getHealth() + RCT_HEAL_PER_TICK);
            p.setHealth(newHp);
        }, 5L, 5L);

        // Stop after duration
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            rctActiveUntilMs.remove(uuid);
            Bukkit.getScheduler().cancelTask(taskId);
            if (p.isOnline()) {
                p.sendMessage(plugin.cfg().prefix() + "§7RCT faded.");
                // Notify when cooldown is almost up (via scheduler)
                Bukkit.getScheduler().runTaskLater(plugin,
                        () -> {
                            if (p.isOnline()) {
                                p.sendMessage(plugin.cfg().prefix() + "§aReverse Cursed Technique is ready");
                            }
                        },
                        (long) (RCT_COOLDOWN_SECONDS - RCT_DURATION_SECONDS) * 20L
                );
            }
        }, (long) RCT_DURATION_SECONDS * 20L);

        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.5f);
    }
}
