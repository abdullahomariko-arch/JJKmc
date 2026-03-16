package me.axebanz.jJK;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * Limitless — placeholder technique.
 * Infinite Void (Mugen) manipulates space and perception.
 */
public final class LimitlessTechnique implements Technique {

    private final JJKCursedToolsPlugin plugin;

    public LimitlessTechnique(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String id() {
        return "limitless";
    }

    @Override
    public String displayName() {
        return "§bLimitless";
    }

    @Override
    public String hexColor() {
        return "#00BFFF";
    }

    @Override
    public String glyphTag() {
        return "";
    }

    @Override
    public String iconColor() {
        return "§b";
    }

    @Override
    public void castAbility(Player player, AbilitySlot slot) {
        switch (slot) {
            case ONE -> {
                // Infinity — brief push back on nearby entities (placeholder)
                Vector dir = player.getLocation().getDirection().clone().normalize();
                player.setVelocity(dir.multiply(0.5));
                player.getWorld().spawnParticle(Particle.END_ROD,
                        player.getLocation().add(0, 1.0, 0), 40, 0.5, 0.5, 0.5, 0.03);
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.6f);
                player.sendMessage(plugin.cfg().prefix() + "§bInfinity");
            }
            case TWO -> {
                // Blue — pull effect (placeholder)
                Vector v = player.getVelocity();
                player.setVelocity(new Vector(v.getX(), 0.5, v.getZ()));
                player.getWorld().spawnParticle(Particle.PORTAL,
                        player.getLocation().add(0, 1.0, 0), 30, 0.3, 0.5, 0.3, 0.01);
                player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.8f, 0.5f);
                player.sendMessage(plugin.cfg().prefix() + "§bBlue");
            }
            case THREE -> {
                // Red — explosive push (placeholder)
                player.setFallDistance(0f);
                player.getWorld().spawnParticle(Particle.EXPLOSION,
                        player.getLocation().add(0, 1.0, 0), 3, 0.3, 0.3, 0.3, 0);
                player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 1.4f);
                player.sendMessage(plugin.cfg().prefix() + "§cRed");
            }
        }
    }
}
