package me.axebanz.jJK;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class BloodManipulationManager {

    private final JJKCursedToolsPlugin plugin;
    private final SupernovaManager supernovaManager;

    // Womb counts per player
    private final Map<UUID, Integer> wombCounts = new ConcurrentHashMap<>();

    // Harpoon tracking: arrow UUID -> shooter UUID
    private final Map<UUID, UUID> harpoonArrows = new ConcurrentHashMap<>();

    // Piercing blood charge tasks
    private final Map<UUID, BukkitTask> piercingChargeTasks = new ConcurrentHashMap<>();
    private final Map<UUID, Long> piercingChargeStart = new ConcurrentHashMap<>();

    // Blood RCT passive cooldown (separate from regular cooldowns to avoid persistence issues)
    private final Map<UUID, Long> rctCooldowns = new ConcurrentHashMap<>();

    public BloodManipulationManager(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
        this.supernovaManager = new SupernovaManager(plugin);
    }

    public SupernovaManager supernova() { return supernovaManager; }

    private boolean hasTechnique(Player p) {
        String id = plugin.techniqueManager().getAssignedId(p.getUniqueId());
        return "blood_manipulation".equalsIgnoreCase(id);
    }

    // ===== SUPERNOVA =====

    public void activateSupernova(Player p) {
        if (!hasTechnique(p)) {
            p.sendMessage(plugin.cfg().prefix() + "§cYou don't have §4Blood Manipulation§c equipped.");
            return;
        }
        if (!isAbilityUnlocked(p, 1)) {
            p.sendMessage(plugin.cfg().prefix() + "§cSupernova not unlocked. Need 1 Cursed Womb.");
            return;
        }
        UUID uuid = p.getUniqueId();
        if (plugin.cooldowns().isOnCooldown(uuid, "bm_supernova")) {
            long rem = plugin.cooldowns().remainingSeconds(uuid, "bm_supernova");
            p.sendMessage(plugin.cfg().prefix() + "§cSupernova on cooldown: §f" + rem + "s");
            return;
        }
        int current = supernovaManager.getOrbCount(p);
        if (current >= 6) {
            p.sendMessage(plugin.cfg().prefix() + "§4Maximum 6 orbs active. Use §c/bloodmanip explode§4 to release them.");
            return;
        }
        plugin.cooldowns().setCooldown(uuid, "bm_supernova", 3);
        supernovaManager.activateOrb(p);
        p.sendMessage(plugin.cfg().prefix() + "§4Supernova orb created §7[" + (current + 1) + "/6]");
        p.playSound(p.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 0.8f, 0.5f);
    }

    public void explodeSupernova(Player p) {
        if (!hasTechnique(p)) {
            p.sendMessage(plugin.cfg().prefix() + "§cYou don't have §4Blood Manipulation§c equipped.");
            return;
        }
        supernovaManager.explodeOrbs(p);
        p.sendMessage(plugin.cfg().prefix() + "§4Supernova DETONATED!");
    }

    // ===== HARPOON =====

    public void castHarpoon(Player p) {
        if (!hasTechnique(p)) {
            p.sendMessage(plugin.cfg().prefix() + "§cYou don't have §4Blood Manipulation§c equipped.");
            return;
        }
        if (!isAbilityUnlocked(p, 6)) {
            p.sendMessage(plugin.cfg().prefix() + "§cHarpoon not unlocked. Need 6 Cursed Wombs.");
            return;
        }
        UUID uuid = p.getUniqueId();
        if (plugin.cooldowns().isOnCooldown(uuid, "bm_harpoon")) {
            long rem = plugin.cooldowns().remainingSeconds(uuid, "bm_harpoon");
            p.sendMessage(plugin.cfg().prefix() + "§cHarpoon on cooldown: §f" + rem + "s");
            return;
        }
        if (!plugin.ce().tryConsume(uuid, 2)) {
            p.sendMessage(plugin.cfg().prefix() + "§cNot enough Cursed Energy.");
            return;
        }
        plugin.cooldowns().setCooldown(uuid, "bm_harpoon", 10);

        Arrow arrow = p.launchProjectile(Arrow.class);
        arrow.setVelocity(p.getEyeLocation().getDirection().multiply(3.0));
        arrow.setPickupStatus(Arrow.PickupStatus.DISALLOWED);
        arrow.setCustomName("§4Blood Harpoon");
        harpoonArrows.put(arrow.getUniqueId(), uuid);

        // Red particle trail task
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            if (arrow.isDead() || !arrow.isValid()) {
                task.cancel();
                return;
            }
            Particle.DustOptions red = new Particle.DustOptions(Color.RED, 1.5f);
            arrow.getWorld().spawnParticle(Particle.DUST, arrow.getLocation(), 5, 0.1, 0.1, 0.1, 0, red);
        }, 0L, 1L);

        p.sendMessage(plugin.cfg().prefix() + "§4Blood Harpoon launched!");
        p.playSound(p.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0f, 0.5f);
    }

    /** Called from BloodManipulationListener when a harpoon arrow hits an entity. */
    public boolean isHarpoonArrow(UUID arrowUUID) {
        return harpoonArrows.containsKey(arrowUUID);
    }

    public void applyHarpoonHit(LivingEntity target, UUID arrowUUID, Player shooter) {
        harpoonArrows.remove(arrowUUID);
        target.damage(15.0, shooter);
        target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 200, 1, false, true, true));
        target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 100, 0, false, true, true));
        target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0, 1, 0), 20,
                0.5, 0.5, 0.5, 0, new Particle.DustOptions(Color.RED, 2.0f));
    }

    // ===== PIERCING BLOOD =====

    public void startPiercingBlood(Player p) {
        if (!hasTechnique(p)) {
            p.sendMessage(plugin.cfg().prefix() + "§cYou don't have §4Blood Manipulation§c equipped.");
            return;
        }
        if (!isAbilityUnlocked(p, 10)) {
            p.sendMessage(plugin.cfg().prefix() + "§cPiercing Blood not unlocked. Need 10 Cursed Wombs.");
            return;
        }
        UUID uuid = p.getUniqueId();
        if (plugin.cooldowns().isOnCooldown(uuid, "bm_piercing")) {
            long rem = plugin.cooldowns().remainingSeconds(uuid, "bm_piercing");
            p.sendMessage(plugin.cfg().prefix() + "§cPiercing Blood on cooldown: §f" + rem + "s");
            return;
        }

        // Cancel existing charge
        BukkitTask old = piercingChargeTasks.remove(uuid);
        if (old != null) old.cancel();

        piercingChargeStart.put(uuid, System.currentTimeMillis());
        p.sendMessage(plugin.cfg().prefix() + "§4Charging Piercing Blood...");

        BukkitTask chargeTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!p.isOnline()) {
                piercingChargeTasks.remove(uuid);
                piercingChargeStart.remove(uuid);
                return;
            }
            long elapsed = System.currentTimeMillis() - piercingChargeStart.getOrDefault(uuid, System.currentTimeMillis());
            int pct = (int) Math.min(100, (elapsed / 3000.0) * 100);
            p.sendActionBar("§4Piercing Blood: §c" + pct + "% charged");
        }, 0L, 5L);
        piercingChargeTasks.put(uuid, chargeTask);

        // Auto-release after 3 seconds
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            BukkitTask ct = piercingChargeTasks.remove(uuid);
            if (ct != null) ct.cancel();
            if (p.isOnline()) releasePiercingBlood(p, 100);
        }, 60L);
    }

    public void releasePiercingBlood(Player p, int chargePct) {
        UUID uuid = p.getUniqueId();

        // Cancel charge task if still running
        BukkitTask ct = piercingChargeTasks.remove(uuid);
        if (ct != null) ct.cancel();
        piercingChargeStart.remove(uuid);

        if (!plugin.ce().tryConsume(uuid, 3)) {
            p.sendMessage(plugin.cfg().prefix() + "§cNot enough Cursed Energy.");
            return;
        }
        plugin.cooldowns().setCooldown(uuid, "bm_piercing", 30);

        double damage = 10.0 + (chargePct * 0.3);
        damage = Math.min(damage, 40.0);
        double finalDamage = damage;

        // Raycast beam
        Location eyeLoc = p.getEyeLocation();
        Vector dir = eyeLoc.getDirection().normalize();

        // Single raycast to find first hit entity along the beam (max 25 blocks)
        RayTraceResult result = eyeLoc.getWorld().rayTraceEntities(eyeLoc, dir, 25.0, 0.6,
                e -> e instanceof LivingEntity && !e.getUniqueId().equals(p.getUniqueId()));

        // Determine particle beam length up to the hit point or max distance
        double beamLength = result != null
                ? result.getHitPosition().distance(eyeLoc.toVector())
                : 25.0;

        // Render particle trail along the full beam
        Particle.DustOptions red = new Particle.DustOptions(Color.RED, 1.5f);
        for (double d = 0.5; d <= beamLength; d += 0.5) {
            eyeLoc.getWorld().spawnParticle(Particle.DUST, eyeLoc.clone().add(dir.clone().multiply(d)),
                    3, 0.1, 0.1, 0.1, 0, red);
        }

        if (result != null && result.getHitEntity() instanceof LivingEntity le) {
            le.damage(finalDamage, p);
            le.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 200, 1, false, true, true));
            le.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 100, 0, false, true, true));
        }
        p.sendMessage(plugin.cfg().prefix() + "§4Piercing Blood! §7(" + (int) damage + " damage)");
    }

    // ===== WOMB SYSTEM =====

    public void giveWomb(Player admin, Player target) {
        target.getInventory().addItem(CursedWombItem.create());
        admin.sendMessage(plugin.cfg().prefix() + "§7Gave Cursed Womb to §4" + target.getName());
        target.sendMessage(plugin.cfg().prefix() + "§4You received a §4§lCursed Womb Painting§4!");
    }

    public void consumeWomb(Player p) {
        UUID uuid = p.getUniqueId();
        int count = wombCounts.merge(uuid, 1, Integer::sum);
        p.sendMessage(plugin.cfg().prefix() + "§4Cursed Womb consumed. §7[" + count + " total]");

        if (count == 1)  p.sendMessage(plugin.cfg().prefix() + "§4Supernova unlocked!");
        if (count == 3)  p.sendMessage(plugin.cfg().prefix() + "§4Blood Reverse Cursed Technique passive unlocked!");
        if (count == 6)  p.sendMessage(plugin.cfg().prefix() + "§4Blood Harpoon unlocked!");
        if (count == 10) p.sendMessage(plugin.cfg().prefix() + "§4Piercing Blood unlocked!");
    }

    public boolean isAbilityUnlocked(Player p, int requiredWombs) {
        return wombCounts.getOrDefault(p.getUniqueId(), 0) >= requiredWombs;
    }

    public int getWombCount(Player p) {
        return wombCounts.getOrDefault(p.getUniqueId(), 0);
    }

    // ===== BLOOD RCT PASSIVE =====

    /** Called from BloodManipulationListener when a blood manipulation player takes damage. */
    public void checkBloodRCT(Player p) {
        if (!hasTechnique(p)) return;
        if (!isAbilityUnlocked(p, 3)) return;

        double maxHp = p.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
        if (p.getHealth() > maxHp * 0.5) return;

        UUID uuid = p.getUniqueId();
        long now = System.currentTimeMillis();
        long cooldownEnd = rctCooldowns.getOrDefault(uuid, 0L);
        if (now < cooldownEnd) return;

        rctCooldowns.put(uuid, now + 45_000L);
        p.sendMessage(plugin.cfg().prefix() + "§4Blood Reverse Cursed Technique activated");

        // Heal 1 heart/sec for 5 seconds
        for (int i = 1; i <= 5; i++) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (p.isOnline()) {
                    double newHp = Math.min(p.getHealth() + 2.0, maxHp);
                    p.setHealth(newHp);
                }
            }, (long) i * 20L);
        }
    }
}
