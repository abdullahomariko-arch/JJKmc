package me.axebanz.jJK;

import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages Deadly Sentencing technique abilities.
 */
public final class DeadlySentencingManager {

    private final JJKCursedToolsPlugin plugin;

    /** Tracks players currently charging the Big Hammer Slam */
    private final Map<UUID, BukkitTask> slamChargeTasks = new ConcurrentHashMap<>();
    private final Map<UUID, Long> slamChargeStartMs = new ConcurrentHashMap<>();

    // CE costs
    private static final int CE_BARRAGE = 2;
    private static final int CE_SLAM = 4;

    public DeadlySentencingManager(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean hasTechnique(Player p) {
        String id = plugin.techniqueManager().getAssignedId(p.getUniqueId());
        return "deadly_sentencing".equalsIgnoreCase(id);
    }

    // ===== HAMMER BARRAGE (Ability 1) =====

    /**
     * Hammer Barrage — 5 rapid hits of 6 damage each to entities near the player.
     */
    public void activateHammerBarrage(Player p) {
        if (!hasTechnique(p)) {
            p.sendMessage(plugin.cfg().prefix() + "§cYou don't have §6Deadly Sentencing§c equipped.");
            return;
        }
        if (plugin.cooldowns().isOnCooldown(p.getUniqueId(), "ds_barrage")) {
            long rem = plugin.cooldowns().remainingSeconds(p.getUniqueId(), "ds_barrage");
            p.sendMessage(plugin.cfg().prefix() + "§cHammer Barrage is on cooldown: §f" + rem + "s");
            return;
        }
        if (!plugin.ce().tryConsume(p.getUniqueId(), CE_BARRAGE)) {
            p.sendMessage(plugin.cfg().prefix() + "§cNot enough Cursed Energy.");
            return;
        }

        plugin.cooldowns().setCooldown(p.getUniqueId(), "ds_barrage", 6);

        p.sendMessage(plugin.cfg().prefix() + "§6Hammer Barrage!");
        p.playSound(p.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 1.0f, 0.7f);

        // 5 hits, one every 4 ticks (0.2 seconds) = 1 second total
        for (int i = 0; i < 5; i++) {
            int hitNum = i;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!p.isOnline()) return;

                // Get closest living entity in front within 4 blocks
                List<LivingEntity> targets = getNearbyTargets(p, 4.0);
                for (LivingEntity target : targets) {
                    target.damage(6.0, p);
                    target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0),
                            8, 0.3, 0.3, 0.3, 0.1);
                }
                p.getWorld().playSound(p.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 0.8f, 1.0f + hitNum * 0.1f);
            }, (long) i * 4);
        }
    }

    // ===== BIG HAMMER SLAM (Ability 2) =====

    /**
     * Starts charging the Big Hammer Slam.
     * Auto-releases after 2 seconds. Player can call releaseHammerSlam() early.
     */
    public void startHammerSlamCharge(Player p) {
        if (!hasTechnique(p)) {
            p.sendMessage(plugin.cfg().prefix() + "§cYou don't have §6Deadly Sentencing§c equipped.");
            return;
        }
        UUID uuid = p.getUniqueId();

        // Cancel existing charge
        BukkitTask old = slamChargeTasks.remove(uuid);
        if (old != null) { old.cancel(); }

        if (plugin.cooldowns().isOnCooldown(uuid, "ds_slam")) {
            long rem = plugin.cooldowns().remainingSeconds(uuid, "ds_slam");
            p.sendMessage(plugin.cfg().prefix() + "§cBig Hammer Slam is on cooldown: §f" + rem + "s");
            return;
        }
        if (!plugin.ce().tryConsume(uuid, CE_SLAM)) {
            p.sendMessage(plugin.cfg().prefix() + "§cNot enough Cursed Energy.");
            return;
        }

        slamChargeStartMs.put(uuid, System.currentTimeMillis());
        p.sendMessage(plugin.cfg().prefix() + "§6Charging Big Hammer Slam... §7(2 seconds)");

        // Charge particle effect
        BukkitTask chargeTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!p.isOnline()) return;
            Particle.DustOptions gold = new Particle.DustOptions(Color.ORANGE, 1.5f);
            p.getWorld().spawnParticle(Particle.DUST, p.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0, gold);
            p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.5f, 0.8f);
        }, 0L, 5L);

        slamChargeTasks.put(uuid, chargeTask);

        // Auto-release after 2 seconds
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            BukkitTask ct = slamChargeTasks.remove(uuid);
            if (ct != null) ct.cancel();
            slamChargeStartMs.remove(uuid);
            if (p.isOnline()) releaseHammerSlam(p);
        }, 40L);
    }

    /**
     * Releases the charged Big Hammer Slam — massive AOE.
     */
    public void releaseHammerSlam(Player p) {
        plugin.cooldowns().setCooldown(p.getUniqueId(), "ds_slam", 15);

        Location center = p.getLocation();
        World w = center.getWorld();
        if (w == null) return;

        p.sendMessage(plugin.cfg().prefix() + "§6§lBIG HAMMER SLAM!");
        w.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.5f);
        w.playSound(center, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.5f, 0.4f);

        // Ground crack particles
        w.spawnParticle(Particle.BLOCK, center, 150, 4.0, 0.1, 4.0, 0,
                Material.STONE.createBlockData());
        w.spawnParticle(Particle.EXPLOSION, center, 5, 3.0, 0.5, 3.0, 0);
        Particle.DustOptions gold = new Particle.DustOptions(Color.ORANGE, 2.5f);
        w.spawnParticle(Particle.DUST, center, 80, 7.5, 1.0, 7.5, 0, gold);

        // Slam ring
        spawnRing(center, 15.0, 100, Color.ORANGE);

        // AOE damage + knockback (15 block radius, 30 damage)
        double radius = 15.0;
        for (LivingEntity le : w.getNearbyLivingEntities(center, radius)) {
            if (le instanceof Player lp && lp.getUniqueId().equals(p.getUniqueId())) continue;

            le.damage(30.0, p);

            // Knockback away + upward
            Vector kb = le.getLocation().toVector().subtract(center.toVector());
            double dist = kb.length();
            if (dist < 0.1) kb = new Vector(0, 1, 0);
            else kb = kb.normalize().multiply(Math.max(0.5, (radius - dist) / radius * 3.0));
            kb.setY(kb.getY() + 1.0);
            le.setVelocity(kb);
        }

        // Screen shake for nearby players
        for (Player nearby : w.getNearbyPlayers(center, radius)) {
            nearby.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.BLINDNESS, 10, 0, false, false, false));
        }
    }

    // ===== DOMAIN EXPANSION =====

    public void expandDomain(Player p) {
        if (!hasTechnique(p)) {
            p.sendMessage(plugin.cfg().prefix() + "§cYou don't have §6Deadly Sentencing§c equipped.");
            return;
        }
        if (plugin.cooldowns().isOnCooldown(p.getUniqueId(), "domain_deadly_sentencing")) {
            long rem = plugin.cooldowns().remainingSeconds(p.getUniqueId(), "domain_deadly_sentencing");
            p.sendMessage(plugin.cfg().prefix() + "§cDomain is on cooldown: §f" + rem + "s");
            return;
        }
        if (!plugin.ce().tryConsume(p.getUniqueId(), 5)) {
            p.sendMessage(plugin.cfg().prefix() + "§cNot enough Cursed Energy.");
            return;
        }

        plugin.cooldowns().setCooldown(p.getUniqueId(), "domain_deadly_sentencing", 180);
        plugin.domainManager().expand(p, new DeadlySentencingDomain(plugin, p));
        p.sendMessage(plugin.cfg().prefix() + "§6§lDeadly Sentencing §7— domain expanded!");
    }

    // ===== Utilities =====

    private List<LivingEntity> getNearbyTargets(Player p, double range) {
        List<LivingEntity> result = new ArrayList<>();
        for (org.bukkit.entity.Entity e : p.getWorld().getNearbyEntities(p.getLocation(), range, range, range)) {
            if (!(e instanceof LivingEntity le)) continue;
            if (e.getUniqueId().equals(p.getUniqueId())) continue;
            // Prefer targets in front of player
            result.add(le);
        }
        // Sort by distance
        result.sort(Comparator.comparingDouble(le -> le.getLocation().distanceSquared(p.getLocation())));
        return result;
    }

    private void spawnRing(Location center, double radius, int points, Color color) {
        World world = center.getWorld();
        if (world == null) return;
        Particle.DustOptions opts = new Particle.DustOptions(color, 1.5f);
        for (int i = 0; i < points; i++) {
            double a = (Math.PI * 2.0) * (i / (double) points);
            double x = Math.cos(a) * radius;
            double z = Math.sin(a) * radius;
            world.spawnParticle(Particle.DUST, center.clone().add(x, 0.2, z), 1, 0, 0, 0, 0, opts);
        }
    }

    // ===== CUSTOM ITEMS =====

    public static ItemStack createJudgesHammer() {
        ItemStack item = new ItemStack(Material.IRON_AXE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6§lJudge's Hammer");
            meta.setCustomModelData(1001);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack createExecutionerSword() {
        ItemStack item = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§4§lExecutioner's Sword");
            meta.setCustomModelData(1002);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static boolean isJudgesHammer(ItemStack item) {
        if (item == null || item.getType() != Material.IRON_AXE) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasCustomModelData() && meta.getCustomModelData() == 1001;
    }

    public static boolean isExecutionerSword(ItemStack item) {
        if (item == null || item.getType() != Material.NETHERITE_SWORD) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasCustomModelData() && meta.getCustomModelData() == 1002;
    }

    public void giveWeapons(Player p) {
        p.getInventory().addItem(createJudgesHammer());
        p.getInventory().addItem(createExecutionerSword());
        p.sendMessage(plugin.cfg().prefix() + "§6Judge's Hammer §7and §4Executioner's Sword §7given!");
    }
}
