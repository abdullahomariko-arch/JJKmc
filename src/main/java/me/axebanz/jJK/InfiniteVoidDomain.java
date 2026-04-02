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

        // Place end portal floor with barrier support
        int cx = center.getBlockX();
        int cy = center.getBlockY() - 1; // floor level
        int cz = center.getBlockZ();
        int floorRadius = RADIUS - 2;

        for (int x = cx - floorRadius; x <= cx + floorRadius; x++) {
            for (int z = cz - floorRadius; z <= cz + floorRadius; z++) {
                double dist = Math.sqrt((x - cx) * (x - cx) + (z - cz) * (z - cz));
                if (dist <= floorRadius) {
                    Location floorLoc = new Location(w, x, cy, z);
                    Location barrierLoc = new Location(w, x, cy - 1, z);

                    // Save originals before overwriting
                    if (!savedBlocks.containsKey(floorLoc)) savedBlocks.put(floorLoc, floorLoc.getBlock().getState());
                    if (!savedBlocks.containsKey(barrierLoc)) savedBlocks.put(barrierLoc, barrierLoc.getBlock().getState());

                    // Place BARRIER below, END_PORTAL on top
                    barrierLoc.getBlock().setType(Material.BARRIER, false);
                    floorLoc.getBlock().setType(Material.END_PORTAL, false);
                }
            }
        }

        w.playSound(center, Sound.BLOCK_END_PORTAL_SPAWN, 1.5f, 0.5f);
        w.playSound(center, Sound.AMBIENT_SOUL_SAND_VALLEY_ADDITIONS, 2.0f, 0.5f);

        // Replace barrier shell blocks with WHITE_CONCRETE for IDG-style visual exterior
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!active) return;
            for (Location loc : barrierBlocks) {
                loc.getBlock().setType(Material.WHITE_CONCRETE, false);
            }
        }, 2L);

        // Notify players inside
        for (Player p : getPlayersInside()) {
            p.sendMessage(plugin.cfg().prefix() + "§1§lINFINITE VOID §0— §7You cannot escape.");
        }
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
