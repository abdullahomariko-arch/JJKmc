package me.axebanz.jJK;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Domain Expansion: Infinite Void
 * All entities inside are completely frozen.
 */
public final class InfiniteVoidDomain extends DomainExpansion {

    private static final int RADIUS = 30;
    private static final double REFINEMENT = 9.0;

    public InfiniteVoidDomain(JJKCursedToolsPlugin plugin, Player caster) {
        super(plugin, caster);
        this.refinement = REFINEMENT;
    }

    @Override public String getName() { return "Infinite Void"; }
    @Override public int getRadius() { return RADIUS; }

    @Override
    public void buildInterior() {
        World w = center.getWorld();
        if (w == null) return;

        Particle.DustOptions blue = new Particle.DustOptions(Color.fromRGB(0, 0, 80), 2.0f);
        Particle.DustOptions purple = new Particle.DustOptions(Color.fromRGB(40, 0, 100), 2.0f);

        // Spawn dense interior particles
        for (int i = 0; i < 200; i++) {
            double r = RADIUS * Math.random();
            double theta = Math.random() * Math.PI * 2;
            double phi = Math.random() * Math.PI;
            double x = r * Math.sin(phi) * Math.cos(theta);
            double y = r * Math.cos(phi);
            double z = r * Math.sin(phi) * Math.sin(theta);
            Location loc = center.clone().add(x, y, z);
            w.spawnParticle(Particle.DUST, loc, 1, 0, 0, 0, 0, i % 2 == 0 ? blue : purple);
            w.spawnParticle(Particle.END_ROD, loc, 1, 0.5, 0.5, 0.5, 0.01);
        }

        w.playSound(center, Sound.BLOCK_END_PORTAL_SPAWN, 1.5f, 0.5f);
        w.playSound(center, Sound.AMBIENT_SOUL_SAND_VALLEY_ADDITIONS, 2.0f, 0.5f);
    }

    @Override
    public void onSureHit(Player target) {
        // Completely freeze the target
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 255, false, false, true));
        target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 40, 255, false, false, true));
        target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 5, false, false, true));
        target.setVelocity(target.getVelocity().multiply(0.0));
    }

    @Override
    public void onTick() {
        World w = center.getWorld();
        if (w == null) return;

        // Spawn ambient particles
        Particle.DustOptions blue = new Particle.DustOptions(Color.fromRGB(0, 0, 80), 1.5f);
        for (int i = 0; i < 30; i++) {
            double r = RADIUS * Math.random();
            double theta = Math.random() * Math.PI * 2;
            double phi = Math.random() * Math.PI;
            double x = r * Math.sin(phi) * Math.cos(theta);
            double y = r * Math.cos(phi);
            double z = r * Math.sin(phi) * Math.sin(theta);
            w.spawnParticle(Particle.DUST, center.clone().add(x, y, z), 1, 0, 0, 0, 0, blue);
        }

        // Notify caster of remaining time (tracked by base-class tick loop)
    }

    @Override
    public void onDomainEnd() {
        // Remove all effects from players inside
        for (Player p : getPlayersInside()) {
            p.removePotionEffect(PotionEffectType.SLOWNESS);
            p.removePotionEffect(PotionEffectType.MINING_FATIGUE);
            p.removePotionEffect(PotionEffectType.BLINDNESS);
            if (!p.equals(caster)) {
                p.sendMessage(plugin.cfg().prefix() + "§7Infinite Void §7has collapsed.");
            }
        }
        caster.sendMessage(plugin.cfg().prefix() + "§1Infinite Void §7collapsed.");
    }

    @Override
    protected int getExpansionTickDelay() {
        return 2; // Faster expansion for the void
    }
}
