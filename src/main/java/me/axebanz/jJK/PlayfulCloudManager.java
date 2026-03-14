package me.axebanz.jJK;

import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayfulCloudManager {

    private final JJKCursedToolsPlugin plugin;

    private static final int MAX_HITS = 15;

    // behavior
    private static final int SPEED_AT = 5;
    private static final int STRENGTH_AT = 10;
    private static final int FINISHER_AT = 15;

    private static final int SPEED_SECONDS = 5;
    private static final int STRENGTH_SECONDS = 2;
    private static final int COOLDOWN_SECONDS = 15;

    // tuning
    private static final double FINISHER_BONUS_DAMAGE = 10.0; // 5 hearts
    private static final double FINISHER_KNOCKBACK_H = 2.2;
    private static final double FINISHER_KNOCKBACK_Y = 0.45;

    // reset if you stop holding / stop hitting
    private static final long INACTIVITY_RESET_MS = 4500;

    private final Map<UUID, Integer> hits = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastHitAtMs = new ConcurrentHashMap<>();
    private final Map<UUID, Long> cooldownUntilMs = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> finisherReady = new ConcurrentHashMap<>();

    public PlayfulCloudManager(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isOnCooldown(UUID u) {
        return cooldownUntilMs.getOrDefault(u, 0L) > System.currentTimeMillis();
    }

    public long remainingCooldownSeconds(UUID u) {
        long rem = Math.max(0L, cooldownUntilMs.getOrDefault(u, 0L) - System.currentTimeMillis());
        return rem / 1000L;
    }

    public void clear(UUID u) {
        hits.remove(u);
        lastHitAtMs.remove(u);
        cooldownUntilMs.remove(u);
        finisherReady.remove(u);
    }

    public void onHit(Player attacker, LivingEntity victim) {
        UUID u = attacker.getUniqueId();
        long now = System.currentTimeMillis();

        // cooldown gate
        if (isOnCooldown(u)) {
            long rem = remainingCooldownSeconds(u);
            plugin.actionbarUI().setTimer(u, "playfulcloud.cd", "■", "§c", rem);
            return;
        }

        // reset if inactive too long
        long last = lastHitAtMs.getOrDefault(u, 0L);
        if (last > 0 && (now - last) > INACTIVITY_RESET_MS) {
            hits.put(u, 0);
            finisherReady.put(u, false);
        }
        lastHitAtMs.put(u, now);

        // finisher consumption
        if (Boolean.TRUE.equals(finisherReady.get(u))) {
            finisherReady.put(u, false);
            hits.put(u, 0);

            // apply finisher effects
            victim.damage(FINISHER_BONUS_DAMAGE, attacker);

            Vector dir = victim.getLocation().toVector().subtract(attacker.getLocation().toVector()).normalize();
            Vector kb = dir.multiply(FINISHER_KNOCKBACK_H);
            kb.setY(FINISHER_KNOCKBACK_Y);
            victim.setVelocity(kb);

            attacker.getWorld().playSound(attacker.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 1.0f, 0.7f);

            // start cooldown
            cooldownUntilMs.put(u, now + COOLDOWN_SECONDS * 1000L);
            plugin.actionbarUI().setTimer(u, "playfulcloud.cd", "■", "§c", COOLDOWN_SECONDS);
            return;
        }

        // normal stacking hit
        int h = Math.min(MAX_HITS, hits.getOrDefault(u, 0) + 1);
        hits.put(u, h);

        // thresholds
        if (h == SPEED_AT) {
            attacker.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, SPEED_SECONDS * 20, 1, false, false, true));
            attacker.getWorld().playSound(attacker.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 1.5f);
        } else if (h == STRENGTH_AT) {
            attacker.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, STRENGTH_SECONDS * 20, 2, false, false, true));
            attacker.getWorld().playSound(attacker.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, 1.8f);
        } else if (h >= FINISHER_AT) {
            finisherReady.put(u, true);
            attacker.getWorld().playSound(attacker.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.7f, 1.6f);
        }

        // render bar
        showBar(attacker, h, MAX_HITS, Boolean.TRUE.equals(finisherReady.get(u)));
    }

    private void showBar(Player p, int value, int max, boolean ready) {
        String filled = "■";
        String empty = "□";

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < max; i++) {
            sb.append(i < value ? "§f" + filled : "§7" + empty);
        }

        String label = ready ? "§6FINISHER" : "§bPlayful Cloud";
        p.sendActionBar(label + " §8|§r " + sb + " §7(" + value + "/" + max + ")");
    }
}