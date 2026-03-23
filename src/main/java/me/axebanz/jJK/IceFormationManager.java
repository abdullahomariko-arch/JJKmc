package me.axebanz.jJK;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class IceFormationManager {

    private final JJKCursedToolsPlugin plugin;

    // Track placed ice blocks for cleanup: location -> original block state
    private final Map<Location, BlockState> placedIceBlocks = new ConcurrentHashMap<>();

    public IceFormationManager(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean hasTechnique(Player p) {
        String id = plugin.techniqueManager().getAssignedId(p.getUniqueId());
        return "ice_formation".equalsIgnoreCase(id);
    }

    // ===== FROST CALM =====

    public void castFrostCalm(Player p) {
        if (!hasTechnique(p)) {
            p.sendMessage(plugin.cfg().prefix() + "§cYou don't have §bIce Formation§c equipped.");
            return;
        }
        UUID uuid = p.getUniqueId();
        if (plugin.cooldowns().isOnCooldown(uuid, "if_frostcalm")) {
            long rem = plugin.cooldowns().remainingSeconds(uuid, "if_frostcalm");
            p.sendMessage(plugin.cfg().prefix() + "§cFrost Calm on cooldown: §f" + rem + "s");
            return;
        }
        if (!plugin.ce().tryConsume(uuid, 2)) {
            p.sendMessage(plugin.cfg().prefix() + "§cNot enough Cursed Energy.");
            return;
        }
        plugin.cooldowns().setCooldown(uuid, "if_frostcalm", 10);

        Location eyeLoc = p.getEyeLocation();
        Vector dir = eyeLoc.getDirection().normalize();
        World w = p.getWorld();

        // Cone of freezing mist: 20 blocks forward, cone expands with distance
        Set<LivingEntity> hit = new HashSet<>();
        for (double dist = 1.0; dist <= 20.0; dist += 0.5) {
            double coneRadius = dist * 0.15 + 0.5;
            Location center = eyeLoc.clone().add(dir.clone().multiply(dist));

            w.spawnParticle(Particle.CLOUD, center, 2, coneRadius * 0.3, 0.1, coneRadius * 0.3, 0.02);
            w.spawnParticle(Particle.SNOWFLAKE, center, 1, coneRadius * 0.2, 0.1, coneRadius * 0.2, 0.05);

            for (Entity e : w.getNearbyEntities(center, coneRadius, 1.0, coneRadius)) {
                if (!(e instanceof LivingEntity le)) continue;
                if (e.getUniqueId().equals(p.getUniqueId())) continue;
                if (hit.contains(le)) continue;
                hit.add(le);

                le.damage(10.0, p);
                le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 255, false, true, true));

                // Place packed ice at target feet
                Location feet = le.getLocation();
                placeIceBlock(feet.clone(), w);
                placeIceBlock(feet.clone().add(1, 0, 0), w);
            }
        }

        w.playSound(p.getLocation(), Sound.BLOCK_POWDER_SNOW_PLACE, 1.5f, 0.6f);
        p.sendMessage(plugin.cfg().prefix() + "§bFrost Calm! §7Hit " + hit.size() + " target(s).");
    }

    // ===== FROST CALM MAX =====

    public void castFrostCalmMax(Player p) {
        if (!hasTechnique(p)) {
            p.sendMessage(plugin.cfg().prefix() + "§cYou don't have §bIce Formation§c equipped.");
            return;
        }
        UUID uuid = p.getUniqueId();
        if (plugin.cooldowns().isOnCooldown(uuid, "if_frostcalmmax")) {
            long rem = plugin.cooldowns().remainingSeconds(uuid, "if_frostcalmmax");
            p.sendMessage(plugin.cfg().prefix() + "§cFrost Calm Max on cooldown: §f" + rem + "s");
            return;
        }
        if (!plugin.ce().tryConsume(uuid, 3)) {
            p.sendMessage(plugin.cfg().prefix() + "§cNot enough Cursed Energy.");
            return;
        }
        plugin.cooldowns().setCooldown(uuid, "if_frostcalmmax", 30);

        Location center = p.getLocation();
        World w = p.getWorld();
        int radius = 15;

        p.sendMessage(plugin.cfg().prefix() + "§b§lFROST CALM MAX!");
        w.playSound(center, Sound.BLOCK_POWDER_SNOW_PLACE, 2.0f, 0.4f);

        // Damage and slow all entities in sphere
        for (Entity e : w.getNearbyEntities(center, radius, radius, radius)) {
            if (!(e instanceof LivingEntity le)) continue;
            if (e.getUniqueId().equals(p.getUniqueId())) continue;
            le.damage(20.0, p);
            le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 255, false, true, true));
        }

        // Turn ground surface to packed ice in radius
        int cx = center.getBlockX(), cy = center.getBlockY(), cz = center.getBlockZ();
        for (int x = cx - radius; x <= cx + radius; x++) {
            for (int z = cz - radius; z <= cz + radius; z++) {
                double dist = Math.sqrt((x - cx) * (x - cx) + (z - cz) * (z - cz));
                if (dist > radius) continue;
                Block ground = w.getBlockAt(x, cy - 1, z);
                if (ground.getType().isSolid() && !ground.getType().equals(Material.PACKED_ICE)) {
                    Location loc = ground.getLocation();
                    if (!placedIceBlocks.containsKey(loc)) {
                        placedIceBlocks.put(loc, ground.getState());
                    }
                    ground.setType(Material.PACKED_ICE, false);
                }
            }
        }

        // Sphere burst particles
        for (int i = 0; i < 200; i++) {
            double theta = Math.random() * Math.PI * 2;
            double phi = Math.random() * Math.PI;
            double rx = radius * Math.sin(phi) * Math.cos(theta);
            double ry = radius * Math.cos(phi);
            double rz = radius * Math.sin(phi) * Math.sin(theta);
            w.spawnParticle(Particle.SNOWFLAKE, center.clone().add(rx, ry, rz), 1, 0, 0, 0, 0.01);
        }

        // Restore ice after 30 seconds
        Bukkit.getScheduler().runTaskLater(plugin, this::restoreIceBlocks, 600L);
    }

    // ===== ICEFALL =====

    public void castIcefall(Player p) {
        if (!hasTechnique(p)) {
            p.sendMessage(plugin.cfg().prefix() + "§cYou don't have §bIce Formation§c equipped.");
            return;
        }
        UUID uuid = p.getUniqueId();
        if (plugin.cooldowns().isOnCooldown(uuid, "if_icefall")) {
            long rem = plugin.cooldowns().remainingSeconds(uuid, "if_icefall");
            p.sendMessage(plugin.cfg().prefix() + "§cIcefall on cooldown: §f" + rem + "s");
            return;
        }
        if (!plugin.ce().tryConsume(uuid, 3)) {
            p.sendMessage(plugin.cfg().prefix() + "§cNot enough Cursed Energy.");
            return;
        }
        plugin.cooldowns().setCooldown(uuid, "if_icefall", 20);

        Block targetBlock = p.getTargetBlock(null, 25);
        Location target = (targetBlock != null) ? targetBlock.getLocation() : null;
        if (target == null) target = p.getLocation().add(p.getLocation().getDirection().multiply(10));
        Location dropZone = target.clone();
        World w = p.getWorld();

        p.sendMessage(plugin.cfg().prefix() + "§bIcefall incoming!");
        w.playSound(p.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.5f, 0.4f);

        final Location finalDropZone = dropZone;

        // Spawn packed-ice falling blocks that hover briefly, then slam down
        for (int i = 0; i < 5; i++) {
            double ox = (Math.random() - 0.5) * 8;
            double oz = (Math.random() - 0.5) * 8;
            Location spawnLoc = finalDropZone.clone().add(ox, 8, oz);
            FallingBlock fb = w.spawnFallingBlock(spawnLoc, Material.PACKED_ICE.createBlockData());
            fb.setVelocity(new Vector(0, 0, 0));

            // Hold the block aloft for 2 seconds via upward micro-velocity
            Bukkit.getScheduler().runTaskTimer(plugin, task -> {
                if (fb.isDead() || !fb.isValid()) { task.cancel(); return; }
                fb.setVelocity(new Vector(0, 0.05, 0));
            }, 0L, 1L);

            // Release and let it fall after 2 seconds
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!fb.isDead() && fb.isValid()) {
                    fb.setVelocity(new Vector(
                            (Math.random() - 0.5) * 0.2,
                            -0.5,
                            (Math.random() - 0.5) * 0.2));
                }
            }, 40L);
        }

        // After 3 seconds: area damage + slowness + icicle rain
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (LivingEntity le : w.getNearbyLivingEntities(finalDropZone, 5, 3, 5)) {
                if (le.getUniqueId().equals(p.getUniqueId())) continue;
                le.damage(15.0, p);
                le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 255, false, true, true));
            }
            w.spawnParticle(Particle.SNOWFLAKE, finalDropZone, 100, 5, 2, 5, 0.1);

            // Icicle rain: 10 pointed dripstone dropped from 30 blocks above
            for (int i = 0; i < 10; i++) {
                double ox = (Math.random() - 0.5) * 10;
                double oz = (Math.random() - 0.5) * 10;
                Location icicleLoc = finalDropZone.clone().add(ox, 30, oz);
                FallingBlock icicle = w.spawnFallingBlock(icicleLoc,
                        Material.POINTED_DRIPSTONE.createBlockData());
                icicle.setDropItem(false);

                int finalI = i;
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    for (LivingEntity le : w.getNearbyLivingEntities(icicle.getLocation(), 1.5, 2, 1.5)) {
                        if (le.getUniqueId().equals(p.getUniqueId())) continue;
                        le.damage(8.0, p);
                    }
                }, 40L + finalI * 2L);
            }
        }, 60L);
    }

    // ===== HELPERS =====

    private void placeIceBlock(Location loc, World w) {
        Block block = w.getBlockAt(loc);
        if (!block.getType().isSolid()) {
            if (!placedIceBlocks.containsKey(loc)) {
                placedIceBlocks.put(loc, block.getState());
            }
            block.setType(Material.PACKED_ICE, false);
            Bukkit.getScheduler().runTaskLater(plugin, () -> restoreBlock(loc), 200L);
        }
    }

    private void restoreBlock(Location loc) {
        BlockState original = placedIceBlocks.remove(loc);
        if (original != null) {
            loc.getBlock().setBlockData(original.getBlockData(), false);
        }
    }

    private void restoreIceBlocks() {
        for (Map.Entry<Location, BlockState> entry : new HashMap<>(placedIceBlocks).entrySet()) {
            entry.getKey().getBlock().setBlockData(entry.getValue().getBlockData(), false);
        }
        placedIceBlocks.clear();
    }
}
