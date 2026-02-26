package me.axebanz.jJK;

import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * Handles special ability effects applied to entities.
 * Bug Fix #9: handleSplitSoulHit safety checks to prevent server crash.
 */
public class AbilityService {
    private final JJKCursedToolsPlugin plugin;

    public AbilityService(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Handle a Split Soul Katana hit on a target.
     * Bug Fix #9:
     * - Skip if target player is in CREATIVE or SPECTATOR mode.
     * - Use Math.max(1.0, ...) to prevent setting health to 0 (which kills them).
     * - Wrap in try-catch to prevent server crash.
     * - For non-player entities, check !target.isInvulnerable() before damaging.
     */
    public void handleSplitSoulHit(Player attacker, Entity target, double damage) {
        try {
            if (target instanceof Player targetPlayer) {
                // Bug Fix #9: Skip creative/spectator players
                GameMode gm = targetPlayer.getGameMode();
                if (gm == GameMode.CREATIVE || gm == GameMode.SPECTATOR) {
                    return;
                }
                double currentHealth = targetPlayer.getHealth();
                double newHealth = Math.max(1.0, currentHealth - damage); // Bug Fix #9: min 1.0
                targetPlayer.setHealth(newHealth);
                attacker.sendMessage(plugin.cfg().prefix() + "§cSplit Soul hits for " + String.format("%.1f", damage) + "!");
            } else if (target instanceof LivingEntity entity) {
                // Bug Fix #9: check invulnerable before damaging
                if (!entity.isInvulnerable()) {
                    entity.damage(damage, attacker);
                }
            }
        } catch (Exception e) {
            // Bug Fix #9: prevent server crash
            plugin.getLogger().warning("Error in handleSplitSoulHit: " + e.getMessage());
        }
    }

    public void applyBindingVow(Player target, int durationTicks) {
        // Apply binding vow effect
        target.sendMessage(plugin.cfg().prefix() + "§5Binding Vow applied!");
    }
}
