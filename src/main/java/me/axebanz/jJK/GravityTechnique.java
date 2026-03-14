package me.axebanz.jJK;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class GravityTechnique implements Technique {

    private final JJKCursedToolsPlugin plugin;

    public GravityTechnique(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String id() {
        return "gravity";
    }

    @Override
    public String displayName() {
        return "Gravity";
    }

    @Override
    public String hexColor() {
        return "#8A2BE2"; // purple
    }

    @Override
    public void castAbility(Player player, AbilitySlot slot) {
        // Placeholder abilities just for testing:
        // Ability 1: small push forward
        // Ability 2: small upward hop
        // Ability 3: brief slow-fall feel (velocity clamp)
        switch (slot) {
            case ONE -> {
                Vector dir = player.getLocation().getDirection().clone().normalize();
                player.setVelocity(dir.multiply(0.8));
                player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation().add(0, 1.0, 0), 30, 0.3, 0.4, 0.3, 0.02);
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 0.9f);
            }
            case TWO -> {
                Vector v = player.getVelocity();
                player.setVelocity(new Vector(v.getX(), 0.7, v.getZ()));
                player.getWorld().spawnParticle(Particle.REVERSE_PORTAL, player.getLocation().add(0, 1.0, 0), 25, 0.25, 0.35, 0.25, 0.01);
                player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_SHOOT, 0.8f, 1.2f);
            }
            case THREE -> {
                // quick "gravity stabilize" effect: clamp falling speed for a moment
                player.setFallDistance(0f);
                player.getWorld().spawnParticle(Particle.DRAGON_BREATH, player.getLocation().add(0, 1.0, 0), 20, 0.25, 0.25, 0.25, 0.01);
                player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.7f, 1.4f);
            }
        }
    }
}
