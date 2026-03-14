package me.axebanz.jJK;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.GameMode;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public final class AbilityService {

    private final JJKCursedToolsPlugin plugin;
    private final ConfigManager cfg;
    private final TechniqueManager techniqueManager;
    private final CursedEnergyManager ce;
    private final CooldownManager cooldowns;
    private final RegenLockManager regenLock;
    private final NullifyManager nullify;
    private final CursedToolFactory tools;
    private final ActionbarUI actionbar;
    private final BossbarUI bossbar;

    private final Random rng = new Random();

    public AbilityService(
            JJKCursedToolsPlugin plugin,
            ConfigManager cfg,
            TechniqueManager techniqueManager,
            CursedEnergyManager ce,
            CooldownManager cooldowns,
            RegenLockManager regenLock,
            NullifyManager nullify,
            CursedToolFactory tools,
            ActionbarUI actionbar,
            BossbarUI bossbar
    ) {
        this.plugin = plugin;
        this.cfg = cfg;
        this.techniqueManager = techniqueManager;
        this.ce = ce;
        this.cooldowns = cooldowns;
        this.regenLock = regenLock;
        this.nullify = nullify;
        this.tools = tools;
        this.actionbar = actionbar;
        this.bossbar = bossbar;
    }

    public void tryUseAbility(Player p, ToolId toolId, ItemStack itemInHand) {
        if (toolId == null) return;

        switch (toolId) {
            case DRAGON_BONE -> useDragonBone(p);
            case KAMUTOKE -> useKamutoke(p);
            case SPLIT_SOUL_KATANA -> {}
            case INVERTED_SPEAR -> {}
            default -> {}
        }
    }

    private void useDragonBone(Player p) {
        String key = "dragon_bone.dash";
        ConfigurationSection sec = cfg.c().getConfigurationSection("tools.dragon_bone");
        if (sec == null) return;

        long cd = sec.getLong("cooldownSeconds", 25);
        int ceCost = sec.getInt("ceCost", 2);

        if (cooldowns.isOnCooldown(p.getUniqueId(), key)) {
            long rem = cooldowns.remainingSeconds(p.getUniqueId(), key);
            actionbar.setTimer(p.getUniqueId(), key, "■", "§6", rem);
            return;
        }

        if (!ce.tryConsume(p.getUniqueId(), ceCost)) {
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.6f);
            return;
        }

        double dashSpeed = sec.getDouble("dash.speed", 1.55);
        int durationTicks = sec.getInt("dash.durationTicks", 14);
        double bonusDamageHearts = sec.getDouble("dash.bonusDamageHearts", 3.0);

        Sound s = safeSound(sec.getString("visuals.sound", "ENTITY_IRON_GOLEM_STEP"), Sound.ENTITY_IRON_GOLEM_STEP);
        Particle particle = safeParticle(sec.getString("visuals.particle", "CRIT"), Particle.CRIT);

        p.getWorld().playSound(p.getLocation(), s, 1.0f, 0.9f);

        Vector direction = p.getLocation().getDirection().normalize().multiply(dashSpeed);
        p.setVelocity(direction);

        for (int i = 0; i < durationTicks; i++) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Location loc = p.getLocation();
                p.getWorld().spawnParticle(particle, loc.add(0, 0.5, 0), 5, 0.3, 0.3, 0.3, 0.05);
            }, i);
        }

        Set<LivingEntity> hitEntities = new HashSet<>();

        int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!p.isOnline()) return;

            Location center = p.getLocation();
            double range = 2.0;

            for (LivingEntity entity : p.getWorld().getLivingEntities()) {
                if (entity.equals(p) || hitEntities.contains(entity)) continue;
                if (entity.getLocation().distance(center) <= range) {
                    hitEntities.add(entity);

                    double dmg = bonusDamageHearts * 2.0;
                    entity.damage(dmg, p);

                    DashState.set(p.getUniqueId(), System.currentTimeMillis() + 1000, bonusDamageHearts);

                    entity.getWorld().spawnParticle(particle, entity.getLocation().add(0, 1.0, 0), 15, 0.4, 0.4, 0.4, 0.1);
                    entity.getWorld().playSound(entity.getLocation(), s, 0.8f, 0.7f);
                }
            }
        }, 0L, 1L);

        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> Bukkit.getScheduler().cancelTask(taskId), durationTicks);

        cooldowns.setCooldown(p.getUniqueId(), key, cd);
        actionbar.setTimer(p.getUniqueId(), key, "■", "§6", cd);
        if (cd >= cfg.cooldownBossbarThreshold()) {
            bossbar.showCooldownBossbar(p, "§6Dragon Bone Dash §8|§r {time}", BarColor.YELLOW, cd);
        }
    }

    // FIXED: Split Soul Katana — only does special true damage effect on PLAYERS.
    // On mobs, it acts as a normal netherite sword (no setHealth, no regen lock, no crash).
    public void handleSplitSoulHit(Player attacker, LivingEntity target) {
        // ONLY apply special effect to players
        if (!(target instanceof Player tp)) {
            // Mobs: do nothing special — the sword acts as a normal netherite sword
            return;
        }

        try {
            ConfigurationSection sec = cfg.c().getConfigurationSection("tools.split_soul_katana");
            if (sec == null) return;

            GameMode gm = tp.getGameMode();
            if (gm == GameMode.CREATIVE || gm == GameMode.SPECTATOR) return;

            if (tp.isInvulnerable()) return;

            double trueDamageHearts = sec.getDouble("trueDamageHearts", 2.5);
            int noRegenSeconds = sec.getInt("noRegenSeconds", 15);

            double dmg = trueDamageHearts * 2.0;

            double newHealth = Math.max(1.0, tp.getHealth() - dmg);
            tp.setHealth(newHealth);
            regenLock.lock(tp.getUniqueId(), noRegenSeconds);

            ItemStack helmet = tp.getInventory().getHelmet();
            if (plugin.tools().identify(helmet) == ToolId.DIVINE_WHEEL) {
                plugin.wheelTierManager().recordCategoryHit(tp.getUniqueId(), AdaptationCategory.TRUE_DAMAGE, dmg, tp);
            }

            Particle particle = safeParticle(sec.getString("visuals.particle", "SWEEP_ATTACK"), Particle.SWEEP_ATTACK);
            Sound sound = safeSound(sec.getString("visuals.sound", "ENTITY_WITHER_HURT"), Sound.ENTITY_WITHER_HURT);

            Location mid = tp.getLocation().clone().add(0, 1.0, 0);
            tp.getWorld().spawnParticle(particle, mid, 12, 0.25, 0.25, 0.25, 0.01);
            tp.getWorld().playSound(tp.getLocation(), sound, 1.0f, 0.9f);

            actionbar.setTimer(tp.getUniqueId(), "split_soul.noregen", "■", "§f", noRegenSeconds);
        } catch (Exception e) {
            plugin.getLogger().warning("Error in handleSplitSoulHit: " + e.getMessage());
        }
    }

    private void useKamutoke(Player p) {
        String key = "kamutoke.storm_call";
        ConfigurationSection sec = cfg.c().getConfigurationSection("tools.kamutoke");
        if (sec == null) return;

        long cd = sec.getLong("cooldownSeconds", 120);
        int ceCost = sec.getInt("ceCost", 3);

        if (cooldowns.isOnCooldown(p.getUniqueId(), key)) {
            long rem = cooldowns.remainingSeconds(p.getUniqueId(), key);
            actionbar.setTimer(p.getUniqueId(), key, "■", "§b", rem);
            return;
        }

        if (!ce.tryConsume(p.getUniqueId(), ceCost)) {
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.6f);
            return;
        }

        int strikes = sec.getInt("lightning.strikes", 7);
        double radius = sec.getDouble("lightning.radius", 7.0);
        double minDamageHearts = sec.getDouble("lightning.minDamageHearts", 3.0);
        int delayTicks = sec.getInt("lightning.strikeDelayTicks", 6);

        Particle cloud = safeParticle(sec.getString("visuals.cloudParticle", "CLOUD"), Particle.CLOUD);
        Sound thunder = safeSound(sec.getString("visuals.sound", "ENTITY_LIGHTNING_BOLT_THUNDER"), Sound.ENTITY_LIGHTNING_BOLT_THUNDER);

        p.getWorld().playSound(p.getLocation(), thunder, 1.0f, 0.8f);

        Location cloudLoc = p.getLocation().clone().add(0, 4.0, 0);
        p.getWorld().spawnParticle(cloud, cloudLoc, 80, 1.2, 0.3, 1.2, 0.02);

        for (int i = 0; i < strikes; i++) {
            int tickDelay = i * delayTicks;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!p.isOnline()) return;

                Location center = p.getLocation();
                Location strikeLoc = center.clone().add(rand(-radius, radius), 0, rand(-radius, radius));
                strikeLoc.setY(center.getY());

                p.getWorld().strikeLightningEffect(strikeLoc);
                p.getWorld().playSound(strikeLoc, thunder, 0.9f, 1.0f);

                for (LivingEntity entity : p.getWorld().getLivingEntities()) {
                    if (entity.equals(p)) continue;
                    if (entity.getLocation().distance(strikeLoc) <= radius) {
                        entity.damage(minDamageHearts * 2.0, p);
                    }
                }
            }, tickDelay);
        }

        cooldowns.setCooldown(p.getUniqueId(), key, cd);
        actionbar.setTimer(p.getUniqueId(), key, "■", "§b", cd);
        if (cd >= cfg.cooldownBossbarThreshold()) {
            bossbar.showCooldownBossbar(p, "§bKamutoke §8|§r {time}", BarColor.BLUE, cd);
        }
    }

    public void handleIsohHit(Player attacker, Player victim) {
        ConfigurationSection sec = cfg.c().getConfigurationSection("tools.inverted_spear");
        if (sec == null) return;

        int nullifySeconds = sec.getInt("nullifySeconds", 600);

        if (plugin.techniqueManager().getAssigned(victim.getUniqueId()) == null) return;
        if (nullify.isNullified(victim.getUniqueId())) return;

        nullify.applyNullify(victim, attacker, nullifySeconds);

        bossbar.showNullifiedTitle(victim);

        String prefix = plugin.cfg().prefix();
        String techniqueColor = plugin.techniqueManager().techniqueColorHex(victim.getUniqueId());
        String legacyColor = HexColor.legacyFromHex(techniqueColor);
        victim.sendMessage(prefix + legacyColor + "Your technique has been NULLIFIED! §7(" + TimeFmt.mmss(nullifySeconds) + ")");
        attacker.sendMessage(prefix + "§aSuccessfully nullified §f" + victim.getName() + "'s§a technique!");

        Particle particle = safeParticle(sec.getString("visuals.particle", "ENCHANT"), Particle.ENCHANT);
        Sound sound = safeSound(sec.getString("visuals.sound", "BLOCK_END_PORTAL_SPAWN"), Sound.BLOCK_END_PORTAL_SPAWN);

        victim.getWorld().spawnParticle(particle, victim.getLocation().clone().add(0, 1.0, 0), 80, 0.7, 0.4, 0.7, 0.02);
        victim.getWorld().playSound(victim.getLocation(), sound, 1.0f, 0.8f);

        actionbar.setTimer(victim.getUniqueId(), "nullified.timer", "■", "§c", nullifySeconds);
        actionbar.setTimer(attacker.getUniqueId(), "isoh.reapply", "■", "§6", nullifySeconds);
    }

    private double rand(double min, double max) {
        return min + (max - min) * rng.nextDouble();
    }

    private Particle safeParticle(String name, Particle fallback) {
        if (name == null || name.isBlank()) return fallback;
        String lowered = name.trim().toLowerCase();
        String keyStr = lowered.contains(":") ? lowered : "minecraft:" + lowered;
        NamespacedKey key = NamespacedKey.fromString(keyStr);
        if (key == null) return fallback;
        Particle p = Registry.PARTICLE_TYPE.get(key);
        return (p != null) ? p : fallback;
    }

    private Sound safeSound(String name, Sound fallback) {
        if (name == null || name.isBlank()) return fallback;
        String lowered = name.trim().toLowerCase();
        String keyStr;
        if (lowered.contains(":")) {
            keyStr = lowered;
        } else if (lowered.contains("_")) {
            keyStr = "minecraft:" + lowered.replace('_', '.');
        } else {
            keyStr = "minecraft:" + lowered;
        }
        NamespacedKey key = NamespacedKey.fromString(keyStr);
        if (key == null) return fallback;
        Sound s = Registry.SOUNDS.get(key);
        return (s != null) ? s : fallback;
    }
}