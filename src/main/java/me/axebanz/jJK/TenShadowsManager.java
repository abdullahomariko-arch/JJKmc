package me.axebanz.jJK;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class TenShadowsManager {

    private final JJKCursedToolsPlugin plugin;
    private final ModelEngineBridge model;

    private final NamespacedKey KEY_SHIKIGAMI;
    private final NamespacedKey KEY_SHIKIGAMI_TYPE;
    private final NamespacedKey KEY_SHIKIGAMI_OWNER;
    private final NamespacedKey KEY_RITUAL_MOB;

    private final Map<UUID, TenShadowsProfile> profiles = new ConcurrentHashMap<>();
    private final Map<UUID, ShikigamiInstance> activeShikigami = new ConcurrentHashMap<>();
    private final Map<UUID, BossBar> ritualBossBars = new ConcurrentHashMap<>();
    private final Map<UUID, BossBar> mahoragaBossBars = new ConcurrentHashMap<>();

    // Mahoraga attack tracking
    private final Map<UUID, Integer> mahoragaAttackCycle = new ConcurrentHashMap<>();
    private final Map<UUID, Long> mahoragaLastAttackMs = new ConcurrentHashMap<>();

    // Rabbit Escape speed boost tracking
    private final Map<UUID, Boolean> rabbitWallActive = new ConcurrentHashMap<>();

    private static final long SUMMON_COOLDOWN_SECONDS = 30;
    private static final long RITUAL_COOLDOWN_SECONDS = 120;
    private static final int SUMMON_CE_COST = 3;
    private static final int RITUAL_CE_COST = 5;
    private static final double SHIKIGAMI_FOLLOW_DISTANCE = 3.5;
    private static final double SHIKIGAMI_ATTACK_RANGE = 3.0;
    private static final double SHIKIGAMI_HARD_LIMIT = 25.0;

    // Mahoraga constants
    private static final double MAHORAGA_ATTACK_RANGE = 4.0;
    private static final long MAHORAGA_ATTACK_COOLDOWN_MS = 1500;
    private static final double MAHORAGA_NORMAL_DAMAGE = 28.0;   // was 12, ~2.3x
    private static final double MAHORAGA_UPPERCUT_DAMAGE = 20.0; // was 8, 2.5x
    private static final double MAHORAGA_DOWNSLAM_DAMAGE = 45.0; // was 18, 2.5x
    // Launch velocity Y to reach ~200 blocks up.
    // Minecraft uses blocks/tick: gravity ~0.08 blocks/tick^2, apex h = v^2/(2*0.08).
    // v = 5.7 -> apex ~203 blocks. Delay ~70 ticks to reach apex.
    private static final double MAHORAGA_UPPERCUT_LAUNCH_Y = 5.7;
    private static final double MAHORAGA_DOWNSLAM_KNOCKBACK = 3.5;
    private static final long MAHORAGA_DOWNSLAM_DELAY_TICKS = 70;
    private static final double MAHORAGA_FOLLOW_SPEED = 0.40;
    private static final double MAHORAGA_CHASE_SPEED = 0.60;
    private static final double MAHORAGA_Y_LERP = 0.25;
    private static final double MAHORAGA_HARD_LIMIT = 25.0;
    private static final double MAHORAGA_FOLLOW_DISTANCE = 3.5;
    /** Uppercut+Downslam special combo fires every N normal attacks. */
    private static final int MAHORAGA_SPECIAL_COMBO_INTERVAL = 4;

    // Rabbit Escape
    private static final int RABBIT_SWARM_COUNT = 65;
    private static final double RABBIT_WALL_RADIUS = 2.5;

    // Divine Dogs
    private static final double DOG_SIDE_OFFSET = 1.5;

    // Nue constants
    private static final double NUE_LIGHTNING_RADIUS = 6.0;
    private static final double NUE_TOTALITY_LIGHTNING_RADIUS = 100.0;
    private static final long NUE_LIGHTNING_COOLDOWN_MS = 4000L;  // was 8000, now more frequent
    private static final double NUE_WING_DAMAGE = 14.0;            // was 6, ~2.3x
    private static final double NUE_LIGHTNING_DAMAGE = 24.0;       // was 10, ~2.4x

    // Toad constants
    private static final long TOAD_TONGUE_COOLDOWN_MS = 3000L;
    private static final double TOAD_TONGUE_RANGE = 20.0;

    // Great Serpent constants
    private static final long SERPENT_GRAB_COOLDOWN_MS = 8000L;
    private static final int SERPENT_SHAKE_DURATION_TICKS = 60; // 3 seconds
    private static final double SERPENT_SHAKE_DAMAGE_PER_TICK = 2.0; // was 0.5, 4x
    private static final double SERPENT_INITIAL_GRAB_DAMAGE = 18.0;  // was 6.0, 3x

    // Max Elephant constants
    private static final long ELEPHANT_CRUSH_COOLDOWN_MS = 10000L;
    private static final int ELEPHANT_CRUSH_MIN_HEIGHT = 20;
    private static final int ELEPHANT_CRUSH_MAX_HEIGHT = 50;
    private static final double ELEPHANT_MIN_DAMAGE = 25.0;  // was 10, 2.5x
    private static final double ELEPHANT_MAX_DAMAGE = 90.0;  // was 40, 2.25x

    // Round Deer constants
    private static final double DEER_HEAL_RADIUS = 5.0;
    private static final long DEER_HEAL_INTERVAL_MS = 500L;

    // Piercing Ox constants
    private static final long OX_CHARGE_COOLDOWN_MS = 5000L;
    private static final double OX_CHARGE_SPEED = 1.2;
    private static final double OX_MIN_DAMAGE = 20.0;  // was 8, 2.5x
    private static final double OX_MAX_DAMAGE = 70.0;  // was 30, ~2.3x
    private static final double OX_MAX_CHARGE_DISTANCE = 50.0; // was 30, increased

    private int brainTaskId = -1;

    public TenShadowsManager(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
        this.model = new ModelEngineBridge(plugin);
        this.KEY_SHIKIGAMI = new NamespacedKey(plugin, "shikigami");
        this.KEY_SHIKIGAMI_TYPE = new NamespacedKey(plugin, "shikigami_type");
        this.KEY_SHIKIGAMI_OWNER = new NamespacedKey(plugin, "shikigami_owner");
        this.KEY_RITUAL_MOB = new NamespacedKey(plugin, "ritual_mob");
    }

    public void start() {
        if (brainTaskId != -1) Bukkit.getScheduler().cancelTask(brainTaskId);
        brainTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::tick, 1L, 1L);
    }

    // ========== Profile Management ==========

    public TenShadowsProfile getProfile(UUID uuid) {
        return profiles.computeIfAbsent(uuid, TenShadowsProfile::new);
    }

    public boolean hasTechnique(Player p) {
        String techId = plugin.techniqueManager().getAssignedId(p.getUniqueId());
        return "ten_shadows".equalsIgnoreCase(techId);
    }

    // ========== Summoning ==========

    public void trySummon(Player p, ShikigamiType type) {
        UUID u = p.getUniqueId();
        String prefix = plugin.cfg().prefix();
        TenShadowsProfile prof = getProfile(u);

        if (!hasTechnique(p)) {
            p.sendMessage(prefix + "§cYou don't have Ten Shadows.");
            return;
        }

        if (!plugin.techniqueManager().canUseTechniqueActions(p, true)) return;

        if (prof.activeSummonId != null) {
            p.sendMessage(prefix + "§cYou already have §f" + ShikigamiType.from(prof.activeSummonId).displayName() + "§c summoned. Dismiss first.");
            return;
        }

        if (prof.ritualActive) {
            p.sendMessage(prefix + "§cYou have a ritual in progress! Finish or cancel it first.");
            return;
        }

        // Check if player can summon this type
        boolean canSummon = prof.isUnlocked(type);
        if (!canSummon) {
            p.sendMessage(prefix + "§cYou haven't unlocked §f" + type.displayName() + "§c yet. Use /tenshadows ritual " + type.id());
            return;
        }

        long now = System.currentTimeMillis();
        if (prof.summonCooldownUntilMs > now) {
            long rem = (prof.summonCooldownUntilMs - now) / 1000L;
            p.sendMessage(prefix + "§cSummon on cooldown: §f" + TimeFmt.mmss(rem));
            return;
        }

        if (!plugin.ce().tryConsume(u, SUMMON_CE_COST)) {
            p.sendMessage(prefix + "§cNot enough Cursed Energy. (Need " + SUMMON_CE_COST + ")");
            return;
        }

        // Mahoraga and Piercing Ox spawn BEHIND the player (opposite of facing direction)
        // All other shikigami spawn 2-3 blocks IN FRONT at ground level, using horizontal direction only.
        Location spawnLoc;
        Vector horizDir = p.getLocation().getDirection().clone().setY(0).normalize();
        if (type == ShikigamiType.MAHORAGA || type == ShikigamiType.PIERCING_OX) {
            spawnLoc = p.getLocation().clone().add(horizDir.clone().multiply(-3));
        } else {
            spawnLoc = p.getLocation().clone().add(horizDir.clone().multiply(2.5));
        }
        spawnLoc.setY(p.getLocation().getY());
        spawnLoc = snapToGround(spawnLoc);

        ShikigamiInstance instance = new ShikigamiInstance(type, u);

        if (type == ShikigamiType.RABBIT_ESCAPE) {
            // Spawn swarm
            spawnRabbitSwarm(p, instance, spawnLoc);
        } else if (type.isPair()) {
            // Spawn pair (Divine Dogs)
            spawnDivineDogsPair(p, instance, spawnLoc);
        } else if (type.usesArmorStandModel()) {
            // Spawn as ArmorStand + ModelEngine (legacy, not used by Mahoraga anymore)
            spawnArmorStandShikigami(p, type, instance, spawnLoc, false);
        } else {
            // Normal mob spawn (includes Mahoraga as Iron Golem)
            LivingEntity entity = spawnMobShikigami(p, type, spawnLoc, false);
            if (entity == null) {
                p.sendMessage(prefix + "§cFailed to summon shikigami.");
                plugin.ce().add(u, SUMMON_CE_COST);
                return;
            }
            instance.setEntityUuid(entity.getUniqueId());
        }

        activeShikigami.put(u, instance);
        prof.activeSummonId = type.id();
        prof.setState(type, ShikigamiState.ACTIVE);
        prof.summonCooldownUntilMs = now + SUMMON_COOLDOWN_SECONDS * 1000L;
        prof.lastSummonMs = now; // track for dismiss cooldown

        if (type == ShikigamiType.MAHORAGA) {
            createMahoragaBossBar(p, instance);
        }

        p.sendMessage(prefix + "§a" + type.displayName() + " §ahas been summoned!");
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.6f, 1.4f);
        spawnSummonParticles(spawnLoc);
    }

    public void dismiss(Player p) {
        UUID u = p.getUniqueId();
        String prefix = plugin.cfg().prefix();
        TenShadowsProfile prof = getProfile(u);

        if (prof.activeSummonId == null) {
            p.sendMessage(prefix + "§cNo shikigami is currently summoned.");
            return;
        }

        ShikigamiType type = ShikigamiType.from(prof.activeSummonId);
        removeActiveShikigami(u, false);

        rabbitWallActive.remove(u);

        p.sendMessage(prefix + "§7" + (type != null ? type.displayName() : "Shikigami") + " §7dismissed.");
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_SOUL_SAND_BREAK, 0.8f, 1.2f);
    }

    private void removeActiveShikigami(UUID ownerUuid, boolean destroyed) {
        ShikigamiInstance instance = activeShikigami.remove(ownerUuid);
        TenShadowsProfile prof = getProfile(ownerUuid);

        if (instance != null) {
            // Clean up Max Elephant water blocks
            for (Location waterLoc : instance.placedWaterBlocks()) {
                Block b = waterLoc.getBlock();
                if (b.getType() == Material.WATER) {
                    b.setType(Material.AIR);
                }
            }
            instance.placedWaterBlocks().clear();

            // Remove main entity
            removeEntity(instance.entityUuid());
            // Remove second entity (Divine Dogs pair)
            removeEntity(instance.secondEntityUuid());
            // Remove swarm entities (Rabbit Escape)
            for (UUID sid : instance.swarmEntityUuids()) {
                removeEntity(sid);
            }
        }

        BossBar bar = mahoragaBossBars.remove(ownerUuid);
        if (bar != null) bar.removeAll();

        if (prof.activeSummonId != null) {
            ShikigamiType type = ShikigamiType.from(prof.activeSummonId);
            if (type != null) {
                // Remove Speed when Rabbit Escape is dismissed
                if (type == ShikigamiType.RABBIT_ESCAPE) {
                    Player owner = Bukkit.getPlayer(ownerUuid);
                    if (owner != null) owner.removePotionEffect(PotionEffectType.SPEED);
                }
                if (destroyed) {
                    handleShikigamiDeath(ownerUuid, type);
                } else {
                    prof.setState(type, ShikigamiState.UNLOCKED);
                }
            }
        }

        prof.activeSummonId = null;
        prof.activeSummonEntityUuid = null;
        mahoragaAttackCycle.remove(ownerUuid);
        mahoragaLastAttackMs.remove(ownerUuid);
        rabbitWallActive.remove(ownerUuid);
    }

    private void removeEntity(UUID uuid) {
        if (uuid == null) return;
        Entity e = Bukkit.getEntity(uuid);
        if (e != null && e.isValid()) {
            model.removeModel(e);
            e.remove();
        }
    }

    // ========== Spawn Methods ==========

    /** Spawn Mahoraga (or any ArmorStand-model shikigami) like Rika */
    private void spawnArmorStandShikigami(Player owner, ShikigamiType type, ShikigamiInstance instance, Location loc, boolean hostile) {
        loc.setYaw(dirToYaw(owner.getLocation().getDirection()));
        loc.setPitch(0);

        ArmorStand as = owner.getWorld().spawn(loc, ArmorStand.class, e -> {
            e.setCustomName((hostile ? "§c" : "") + type.displayName());
            e.setCustomNameVisible(true);
            e.setInvisible(true);
            e.setMarker(false);
            e.setSmall(false);
            e.setGravity(false);
            e.setSilent(true);
            e.setInvulnerable(false); // Can take damage during rituals
            e.setCanPickupItems(false);
            e.setCollidable(false);
            e.setRemoveWhenFarAway(false);
        });

        as.setRotation(loc.getYaw(), 0);
        as.getPersistentDataContainer().set(KEY_SHIKIGAMI, PersistentDataType.INTEGER, 1);
        as.getPersistentDataContainer().set(KEY_SHIKIGAMI_TYPE, PersistentDataType.STRING, type.id());
        as.getPersistentDataContainer().set(KEY_SHIKIGAMI_OWNER, PersistentDataType.STRING, owner.getUniqueId().toString());
        if (hostile) {
            as.getPersistentDataContainer().set(KEY_RITUAL_MOB, PersistentDataType.INTEGER, 1);
        }

        instance.setEntityUuid(as.getUniqueId());

        // Apply ModelEngine model
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!as.isValid()) return;
            model.applyModel(as, type.modelId());
            model.playAnimation(as, "summon");
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (as.isValid()) model.playAnimation(as, "idle");
            }, 20L);
        }, 1L);

        // Track health via a hidden entity or metadata for ArmorStand-based shikigami
        // We use a custom health tracker in the profile
        TenShadowsProfile prof = getProfile(owner.getUniqueId());
        prof.armorStandHealth = type.maxHealth();
        prof.armorStandMaxHealth = type.maxHealth();

        if (type == ShikigamiType.MAHORAGA) {
            mahoragaAttackCycle.put(as.getUniqueId(), 0);
            mahoragaLastAttackMs.put(as.getUniqueId(), 0L);
        }
    }

    /** Spawn Divine Dogs as a pair */
    private void spawnDivineDogsPair(Player owner, ShikigamiInstance instance, Location baseLoc) {
        Vector right = getPerpendicularRight(owner);

        Location leftLoc = baseLoc.clone().add(right.clone().multiply(-DOG_SIDE_OFFSET));
        Location rightLoc = baseLoc.clone().add(right.clone().multiply(DOG_SIDE_OFFSET));
        leftLoc = snapToGround(leftLoc);
        rightLoc = snapToGround(rightLoc);

        LivingEntity dog1 = spawnMobShikigami(owner, ShikigamiType.DIVINE_DOGS, leftLoc, false);
        LivingEntity dog2 = spawnMobShikigami(owner, ShikigamiType.DIVINE_DOGS, rightLoc, false);

        if (dog1 != null) {
            dog1.setCustomName("§fDivine Dog §8(White)");
            instance.setEntityUuid(dog1.getUniqueId());
        }
        if (dog2 != null) {
            dog2.setCustomName("§8Divine Dog §f(Black)");
            instance.setSecondEntityUuid(dog2.getUniqueId());
        }
    }

    /** Spawn Rabbit Escape as a swarm of 65 rabbits in tiers around the owner */
    private void spawnRabbitSwarm(Player owner, ShikigamiInstance instance, Location baseLoc) {
        // Tier layout: inner ring (8 rabbits), middle ring (16), outer ring (24), top layer (17)
        // Total: 65 rabbits arranged in concentric rings
        int[] tierCounts = {8, 16, 24, 17};
        double[] tierRadii = {1.2, 2.0, 2.8, 1.8};
        double[] tierYOffsets = {0.0, 0.0, 0.0, 0.8};
        int globalIdx = 0;

        for (int tier = 0; tier < tierCounts.length; tier++) {
            int count = tierCounts[tier];
            double radius = tierRadii[tier];
            double yOffset = tierYOffsets[tier];
            for (int i = 0; i < count; i++) {
                double angle = (Math.PI * 2.0) * (i / (double) count);
                Location rabbitLoc = baseLoc.clone().add(
                    Math.cos(angle) * radius, yOffset, Math.sin(angle) * radius);
                rabbitLoc = snapToGround(rabbitLoc);
                rabbitLoc.add(0, yOffset, 0);

                Rabbit rabbit = (Rabbit) owner.getWorld().spawnEntity(rabbitLoc, EntityType.RABBIT);
                rabbit.setCustomName("§fRabbit Escape");
                rabbit.setCustomNameVisible(false);
                rabbit.setInvulnerable(true);
                rabbit.setSilent(true);

                rabbit.getPersistentDataContainer().set(KEY_SHIKIGAMI, PersistentDataType.INTEGER, 1);
                rabbit.getPersistentDataContainer().set(KEY_SHIKIGAMI_TYPE, PersistentDataType.STRING, ShikigamiType.RABBIT_ESCAPE.id());
                rabbit.getPersistentDataContainer().set(KEY_SHIKIGAMI_OWNER, PersistentDataType.STRING, owner.getUniqueId().toString());

                instance.swarmEntityUuids().add(rabbit.getUniqueId());
                if (globalIdx == 0) instance.setEntityUuid(rabbit.getUniqueId());
                globalIdx++;
            }
        }

        // Give owner speed boost while rabbits are active
        owner.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 2, false, false, true));
        rabbitWallActive.put(owner.getUniqueId(), true);

        owner.sendMessage(plugin.cfg().prefix() + "§fRabbit Escape! §7The swarm of 65 rabbits surrounds and protects you!");
    }

    /**
     * Spawn Rabbit Escape RITUAL: 65 rabbits scattered around the area.
     * One white rabbit is the target (can be killed). All other brown rabbits are invulnerable.
     * Rabbits have Speed so they run away — it's a chase game.
     * Returns the UUID string of the white target rabbit, or null on failure.
     */
    private String spawnRabbitEscapeRitual(Player owner, Location baseLoc) {
        World w = baseLoc.getWorld();
        if (w == null) return null;

        // Spawn 64 brown invulnerable rabbits scattered around
        for (int i = 0; i < 64; i++) {
            double angle = (Math.PI * 2.0) * i / 64.0;
            double radius = 3.0 + Math.random() * 8.0;
            Location loc = baseLoc.clone().add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
            loc = snapToGround(loc);

            Rabbit rabbit = (Rabbit) w.spawnEntity(loc, EntityType.RABBIT);
            rabbit.setRabbitType(Rabbit.Type.BROWN);
            rabbit.setCustomName("§7Rabbit");
            rabbit.setCustomNameVisible(false);
            rabbit.setInvulnerable(true);
            rabbit.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 3, false, false, false));

            rabbit.getPersistentDataContainer().set(KEY_SHIKIGAMI, PersistentDataType.INTEGER, 1);
            rabbit.getPersistentDataContainer().set(KEY_SHIKIGAMI_TYPE, PersistentDataType.STRING, ShikigamiType.RABBIT_ESCAPE.id());
            rabbit.getPersistentDataContainer().set(KEY_SHIKIGAMI_OWNER, PersistentDataType.STRING, owner.getUniqueId().toString());
            rabbit.getPersistentDataContainer().set(KEY_RITUAL_MOB, PersistentDataType.INTEGER, 1);
        }

        // Spawn 1 WHITE rabbit — this is the target
        Location whiteLoc = baseLoc.clone().add((Math.random() - 0.5) * 10, 0, (Math.random() - 0.5) * 10);
        whiteLoc = snapToGround(whiteLoc);

        Rabbit whiteRabbit = (Rabbit) w.spawnEntity(whiteLoc, EntityType.RABBIT);
        whiteRabbit.setRabbitType(Rabbit.Type.WHITE);
        whiteRabbit.setCustomName("§f§l⚡ Target");
        whiteRabbit.setCustomNameVisible(true);
        whiteRabbit.setInvulnerable(false); // can be killed
        whiteRabbit.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 3, false, false, false));
        whiteRabbit.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(10.0);
        whiteRabbit.setHealth(10.0);

        whiteRabbit.getPersistentDataContainer().set(KEY_SHIKIGAMI, PersistentDataType.INTEGER, 1);
        whiteRabbit.getPersistentDataContainer().set(KEY_SHIKIGAMI_TYPE, PersistentDataType.STRING, ShikigamiType.RABBIT_ESCAPE.id());
        whiteRabbit.getPersistentDataContainer().set(KEY_SHIKIGAMI_OWNER, PersistentDataType.STRING, owner.getUniqueId().toString());
        whiteRabbit.getPersistentDataContainer().set(KEY_RITUAL_MOB, PersistentDataType.INTEGER, 1);

        owner.sendMessage(plugin.cfg().prefix() + "§fRabbit Escape Ritual! §7Find and kill the §f§lwhite rabbit§7!");
        return whiteRabbit.getUniqueId().toString();
    }


        EntityType entityType = getBaseEntityType(type);
        LivingEntity entity = (LivingEntity) loc.getWorld().spawnEntity(loc, entityType);

        if (type.maxHealth() > 0) {
            entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(type.maxHealth());
            entity.setHealth(type.maxHealth());
        }

        entity.setCustomName((hostile ? "§c" : "") + type.displayName());
        entity.setCustomNameVisible(true);

        entity.getPersistentDataContainer().set(KEY_SHIKIGAMI, PersistentDataType.INTEGER, 1);
        entity.getPersistentDataContainer().set(KEY_SHIKIGAMI_TYPE, PersistentDataType.STRING, type.id());
        entity.getPersistentDataContainer().set(KEY_SHIKIGAMI_OWNER, PersistentDataType.STRING, owner.getUniqueId().toString());
        if (hostile) {
            entity.getPersistentDataContainer().set(KEY_RITUAL_MOB, PersistentDataType.INTEGER, 1);
        }

        // Mahoraga: use Iron Golem — invisible body, silent, ModelEngine overlay
        if (type == ShikigamiType.MAHORAGA && entity instanceof IronGolem golem) {
            golem.setSilent(true);
            golem.setRemoveWhenFarAway(false);
            // Make it passive towards owner; will be controlled by AI tick
            golem.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false, false));
        }

        if (hostile && entity instanceof Mob mob) {
            mob.setTarget(owner);
        }
        if (!hostile && entity instanceof Mob mob) {
            mob.setAware(true);
        }

        // Round Deer: set as adult horse
        if (type == ShikigamiType.ROUND_DEER && entity instanceof Horse horse) {
            horse.setAdult();
            horse.setTamed(false);
        }

        // Apply model if the type has one
        String modelId = type.modelId();
        if (modelId != null) {
            boolean isRitual = hostile;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!entity.isValid()) return;
                model.applyModel(entity, modelId);
                if (type == ShikigamiType.MAHORAGA) {
                    // Play ritual animation during ritual, then idle; play summon on normal summon
                    model.playAnimation(entity, isRitual ? "ritual" : "summon");
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (entity.isValid()) model.playAnimation(entity, "idle");
                    }, 40L);
                }
            }, 1L);
        }

        return entity;
    }

    private EntityType getBaseEntityType(ShikigamiType type) {
        return switch (type) {
            case DIVINE_DOGS, DIVINE_DOG_TOTALITY -> EntityType.WOLF;
            case TOAD -> EntityType.FROG;
            case RABBIT_ESCAPE -> EntityType.RABBIT;
            case NUE -> EntityType.PHANTOM;
            case GREAT_SERPENT -> EntityType.RAVAGER;
            case MAX_ELEPHANT -> EntityType.RAVAGER;
            case NUE_TOTALITY -> EntityType.PHANTOM;
            case ROUND_DEER -> EntityType.HORSE;
            case PIERCING_OX -> EntityType.RAVAGER;
            case TIGER_FUNERAL -> EntityType.RAVAGER;
            case MAHORAGA -> EntityType.IRON_GOLEM;
        };
    }

    // ========== Shikigami Death & Fusion ==========

    private void handleShikigamiDeath(UUID ownerUuid, ShikigamiType type) {
        TenShadowsProfile prof = getProfile(ownerUuid);
        Player owner = Bukkit.getPlayer(ownerUuid);
        String prefix = plugin.cfg().prefix();

        // Indestructible shikigami (Toad, Nue, Rabbit Escape) → cooldown only, never permanently destroyed
        if (type.isIndestructible()) {
            // Put back to unlocked state (on cooldown via summonCooldownUntilMs)
            prof.setState(type, ShikigamiState.UNLOCKED);
            prof.summonCooldownUntilMs = System.currentTimeMillis() + 60_000L; // 60s cooldown
            if (owner != null) {
                owner.sendMessage(prefix + type.displayName() + " §7was defeated! On cooldown for §f1 minute§7.");
                owner.playSound(owner.getLocation(), Sound.ENTITY_PHANTOM_DEATH, 0.8f, 1.0f);
            }
            // Nue special: even though indestructible, it CAN still contribute to Nue Totality fusion
            // if it was killed in combat (i.e., destroyed=true was triggered from somewhere else).
            // We still check fusion here if nueDestroyed flag gets set.
            return;
        }

        prof.setState(type, ShikigamiState.DESTROYED);

        switch (type) {
            case DIVINE_DOGS -> {
                prof.setState(ShikigamiType.DIVINE_DOG_TOTALITY, ShikigamiState.FUSED_UNLOCKED);
                if (owner != null) {
                    owner.sendMessage(prefix + "§8§l✦ §fDivine Dogs §7has been destroyed...");
                    owner.sendMessage(prefix + "§8§l✦ §8Divine Dog: Totality §fhas been unlocked through fusion!");
                    owner.playSound(owner.getLocation(), Sound.ENTITY_WITHER_DEATH, 0.5f, 1.6f);
                }
            }
            case GREAT_SERPENT -> {
                prof.greatSerpentDestroyed = true;
                if (prof.nueDestroyed) {
                    prof.setState(ShikigamiType.NUE_TOTALITY, ShikigamiState.FUSED_UNLOCKED);
                    if (owner != null) {
                        owner.sendMessage(prefix + "§6§l✦ Nue: Totality §fhas been unlocked through fusion!");
                        owner.playSound(owner.getLocation(), Sound.ENTITY_WITHER_DEATH, 0.5f, 1.6f);
                    }
                } else if (owner != null) {
                    owner.sendMessage(prefix + "§2" + type.displayName() + " §7has been permanently destroyed.");
                }
            }
            default -> {
                if (owner != null) {
                    owner.sendMessage(prefix + type.displayName() + " §7has been permanently destroyed.");
                }
            }
        }
    }

    // ========== Ritual System ==========

    public void startRitual(Player p, ShikigamiType type) {
        UUID u = p.getUniqueId();
        String prefix = plugin.cfg().prefix();
        TenShadowsProfile prof = getProfile(u);

        if (!hasTechnique(p)) {
            p.sendMessage(prefix + "§cYou don't have Ten Shadows.");
            return;
        }

        if (!plugin.techniqueManager().canUseTechniqueActions(p, true)) return;

        if (!type.requiresRitual()) {
            p.sendMessage(prefix + "§c" + type.displayName() + " §cdoesn't require a ritual.");
            return;
        }

        if (prof.isUnlocked(type) || prof.isDestroyed(type)) {
            p.sendMessage(prefix + "§cYou've already dealt with §f" + type.displayName() + "§c.");
            return;
        }

        if (type == ShikigamiType.DIVINE_DOG_TOTALITY || type == ShikigamiType.NUE_TOTALITY) {
            p.sendMessage(prefix + "§c" + type.displayName() + " §ccan only be obtained through fusion.");
            return;
        }

        if (prof.ritualActive) {
            p.sendMessage(prefix + "§cA ritual is already in progress!");
            return;
        }

        if (prof.activeSummonId != null) {
            p.sendMessage(prefix + "§cDismiss your current shikigami before starting a ritual.");
            return;
        }

        long now = System.currentTimeMillis();
        if (prof.ritualCooldownUntilMs > now) {
            long rem = (prof.ritualCooldownUntilMs - now) / 1000L;
            p.sendMessage(prefix + "§cRitual on cooldown: §f" + TimeFmt.mmss(rem));
            return;
        }

        if (!plugin.ce().tryConsume(u, RITUAL_CE_COST)) {
            p.sendMessage(prefix + "§cNot enough Cursed Energy. (Need " + RITUAL_CE_COST + ")");
            return;
        }

        Location spawnLoc = p.getLocation().clone().add(p.getLocation().getDirection().clone().setY(0).normalize().multiply(4));
        spawnLoc.setY(p.getLocation().getY());
        spawnLoc = snapToGround(spawnLoc);

        if (type == ShikigamiType.RABBIT_ESCAPE) {
            // Rabbit Escape ritual: 65 rabbits, 1 white target, the rest brown and invulnerable
            String whiteUuid = spawnRabbitEscapeRitual(p, spawnLoc);
            if (whiteUuid == null) {
                p.sendMessage(prefix + "§cFailed to start ritual.");
                plugin.ce().add(u, RITUAL_CE_COST);
                return;
            }
            prof.ritualEntityUuid = whiteUuid;
        } else if (type.usesArmorStandModel()) {
            // ArmorStand-based (legacy path, kept for compatibility)
            ShikigamiInstance instance = new ShikigamiInstance(type, u);
            spawnArmorStandShikigami(p, type, instance, spawnLoc, true);
            prof.ritualEntityUuid = instance.entityUuid().toString();
            prof.armorStandHealth = type.maxHealth();
            prof.armorStandMaxHealth = type.maxHealth();
        } else {
            // Normal mob ritual
            LivingEntity entity = spawnMobShikigami(p, type, spawnLoc, true);
            if (entity == null) {
                p.sendMessage(prefix + "§cFailed to start ritual.");
                plugin.ce().add(u, RITUAL_CE_COST);
                return;
            }
            prof.ritualEntityUuid = entity.getUniqueId().toString();
        }

        prof.ritualActive = true;
        prof.ritualTargetId = type.id();
        prof.ritualCooldownUntilMs = now + RITUAL_COOLDOWN_SECONDS * 1000L;

        BossBar bossBar = Bukkit.createBossBar(
                type == ShikigamiType.MAHORAGA ? "§5§lMahoraga" : "§c" + type.displayName() + " §8— Ritual",
                type == ShikigamiType.MAHORAGA ? BarColor.PURPLE : BarColor.RED,
                BarStyle.SOLID
        );
        bossBar.setProgress(1.0);
        bossBar.addPlayer(p);
        ritualBossBars.put(u, bossBar);

        p.sendTitle(
                type == ShikigamiType.MAHORAGA ? "§5§lMAHORAGA" : "§c§lRITUAL",
                "§7Defeat §f" + type.displayName() + " §7to tame it!",
                10, 60, 20
        );

        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.6f);
        spawnRitualParticles(spawnLoc);

        p.sendMessage(prefix + "§c§lThe ritual begins! §7Defeat §f" + type.displayName() + " §7to unlock it!");
    }

    /** Called when a ritual mob dies — either via EntityDeathEvent or ArmorStand damage tracking */
    public void onRitualMobDeath(UUID ownerUuid, ShikigamiType type) {
        TenShadowsProfile prof = getProfile(ownerUuid);
        Player owner = Bukkit.getPlayer(ownerUuid);
        String prefix = plugin.cfg().prefix();

        // Clean up ritual entity
        if (prof.ritualEntityUuid != null) {
            removeEntity(parseUuid(prof.ritualEntityUuid));
        }

        // For Rabbit Escape ritual: also remove all brown ritual rabbits in nearby world
        if (type == ShikigamiType.RABBIT_ESCAPE && owner != null) {
            for (Entity e : owner.getWorld().getNearbyEntities(owner.getLocation(), 50, 20, 50)) {
                if (isShikigamiEntity(e) && isRitualMob(e)) {
                    UUID eOwner = getShikigamiOwner(e);
                    if (ownerUuid.equals(eOwner)) e.remove();
                }
            }
        }

        prof.ritualActive = false;
        prof.ritualTargetId = null;
        prof.ritualEntityUuid = null;
        ritualInstances.remove(ownerUuid);

        BossBar bar = ritualBossBars.remove(ownerUuid);
        if (bar != null) bar.removeAll();

        prof.setState(type, ShikigamiState.UNLOCKED);

        if (owner != null) {
            owner.sendTitle("§a§l✦ TAMED ✦", type.displayName() + " §7has been unlocked!", 10, 60, 20);
            owner.sendMessage(prefix + "§a§l✦ " + type.displayName() + " §a§lhas been tamed! You can now summon it.");
            owner.playSound(owner.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);
            owner.setHealth(Math.min(owner.getMaxHealth(), owner.getHealth() + 10));
        }
    }

    /** Deal damage to an ArmorStand-based shikigami (Mahoraga) */
    public void damageArmorStandShikigami(UUID ownerUuid, double damage) {
        TenShadowsProfile prof = getProfile(ownerUuid);
        prof.armorStandHealth -= damage;

        if (prof.armorStandHealth <= 0) {
            prof.armorStandHealth = 0;

            if (prof.ritualActive) {
                ShikigamiType type = ShikigamiType.from(prof.ritualTargetId);
                if (type != null) {
                    onRitualMobDeath(ownerUuid, type);
                }
            } else {
                removeActiveShikigami(ownerUuid, true);
            }
        }
    }

    public void cancelRitual(Player p) {
        UUID u = p.getUniqueId();
        TenShadowsProfile prof = getProfile(u);
        String prefix = plugin.cfg().prefix();

        if (!prof.ritualActive) {
            p.sendMessage(prefix + "§cNo active ritual to cancel.");
            return;
        }

        removeEntity(parseUuid(prof.ritualEntityUuid));

        BossBar bar = ritualBossBars.remove(u);
        if (bar != null) bar.removeAll();

        prof.ritualActive = false;
        prof.ritualTargetId = null;
        prof.ritualEntityUuid = null;

        p.sendMessage(prefix + "§7Ritual cancelled.");
    }

    // ========== Mahoraga Boss Bar ==========

    private void createMahoragaBossBar(Player owner, ShikigamiInstance instance) {
        BossBar bar = Bukkit.createBossBar("§5§lMahoraga", BarColor.PURPLE, BarStyle.SEGMENTED_10);
        bar.setProgress(1.0);
        bar.addPlayer(owner);

        for (Player nearby : owner.getWorld().getPlayers()) {
            if (nearby.getLocation().distanceSquared(owner.getLocation()) <= 50 * 50) {
                bar.addPlayer(nearby);
            }
        }

        mahoragaBossBars.put(owner.getUniqueId(), bar);
    }

    // ========== Mahoraga AI (ArmorStand-based, like Rika) ==========

    private void tickMahoragaArmorStand(UUID ownerUuid, ShikigamiInstance instance, boolean isRitual) {
        if (instance.entityUuid() == null) return;
        Entity entity = Bukkit.getEntity(instance.entityUuid());
        if (entity == null || !entity.isValid()) return;
        if (!(entity instanceof ArmorStand as)) return;

        Player owner = Bukkit.getPlayer(ownerUuid);
        if (owner == null || !owner.isOnline()) return;

        Location rl = as.getLocation();

        if (isRitual) {
            // HOSTILE: chase and attack owner
            double dist = rl.distance(owner.getLocation());

            if (dist > MAHORAGA_HARD_LIMIT) {
                Location beside = snapToGround(owner.getLocation().clone().add(2, 0, 2));
                beside.setYaw(dirToYaw(owner.getLocation().toVector().subtract(beside.toVector())));
                teleportWithYaw(as, beside);
                return;
            }

            if (dist > MAHORAGA_ATTACK_RANGE) {
                // Chase
                Vector toOwner = owner.getLocation().toVector().subtract(rl.toVector());
                toOwner.setY(0);
                if (toOwner.lengthSquared() > 0.01) {
                    double moveAmount = Math.min(MAHORAGA_CHASE_SPEED, dist - MAHORAGA_ATTACK_RANGE + 0.5);
                    Location desired = rl.clone().add(toOwner.normalize().multiply(moveAmount));
                    Location ground = snapToGround(desired);
                    double smoothY = lerpY(rl.getY(), ground.getY());
                    desired.setY(smoothY);
                    desired.setYaw(dirToYaw(toOwner.normalize()));
                    desired.setPitch(0);
                    teleportWithYaw(as, desired);
                }
            }

            // Attack owner if in range
            if (dist <= MAHORAGA_ATTACK_RANGE) {
                tickMahoragaAttackArmorStand(as, owner, ownerUuid, instance, true);
            }
        } else {
            // FRIENDLY: follow owner, attack nearest hostile
            LivingEntity target = findNearestHostile(owner, rl, 18.0);

            if (target != null) {
                double dist = rl.distance(target.getLocation());
                if (dist > MAHORAGA_ATTACK_RANGE) {
                    Vector toTarget = target.getLocation().toVector().subtract(rl.toVector());
                    toTarget.setY(0);
                    double moveAmount = Math.min(MAHORAGA_CHASE_SPEED, dist - MAHORAGA_ATTACK_RANGE + 0.5);
                    if (moveAmount > 0.01) {
                        Location desired = rl.clone().add(toTarget.normalize().multiply(moveAmount));
                        Location ground = snapToGround(desired);
                        double smoothY = lerpY(rl.getY(), ground.getY());
                        desired.setY(smoothY);
                        desired.setYaw(dirToYaw(toTarget.normalize()));
                        desired.setPitch(0);
                        teleportWithYaw(as, desired);
                    }
                }
                if (dist <= MAHORAGA_ATTACK_RANGE) {
                    tickMahoragaAttackArmorStand(as, target, ownerUuid, instance, false);
                }
            } else {
                // Follow owner
                double dist = rl.distance(owner.getLocation());
                if (dist > MAHORAGA_HARD_LIMIT) {
                    Location beside = snapToGround(owner.getLocation().clone().add(1.5, 0, 1.5));
                    beside.setYaw(dirToYaw(owner.getLocation().getDirection()));
                    teleportWithYaw(as, beside);
                } else if (dist > MAHORAGA_FOLLOW_DISTANCE) {
                    Vector toOwner = owner.getLocation().toVector().subtract(rl.toVector());
                    toOwner.setY(0);
                    Vector step = toOwner.normalize().multiply(Math.min(MAHORAGA_FOLLOW_SPEED, dist - MAHORAGA_FOLLOW_DISTANCE + 0.1));
                    Location desired = rl.clone().add(step);
                    Location ground = snapToGround(desired);
                    double smoothY = lerpY(rl.getY(), ground.getY());
                    desired.setY(smoothY);
                    desired.setYaw(dirToYaw(toOwner.normalize()));
                    desired.setPitch(0);
                    teleportWithYaw(as, desired);
                } else {
                    Location adjusted = rl.clone();
                    adjusted.setYaw(dirToYaw(owner.getLocation().getDirection()));
                    adjusted.setPitch(0);
                    Location ground = snapToGround(adjusted);
                    double smoothY = lerpY(rl.getY(), ground.getY());
                    adjusted.setY(smoothY);
                    teleportWithYaw(as, adjusted);
                }
            }

            // Update Mahoraga boss bar
            TenShadowsProfile prof = getProfile(ownerUuid);
            BossBar bar = mahoragaBossBars.get(ownerUuid);
            if (bar != null && prof.armorStandMaxHealth > 0) {
                double pct = prof.armorStandHealth / prof.armorStandMaxHealth;
                bar.setProgress(Math.max(0, Math.min(1, pct)));

                // Show adaptation tier on boss bar
                bar.setTitle("§5§lMahoraga §8| " + instance.currentTier().displayName());
            }
        }
    }

    private void tickMahoragaAttackArmorStand(ArmorStand mahoraga, LivingEntity target, UUID ownerUuid, ShikigamiInstance instance, boolean isRitual) {
        long now = System.currentTimeMillis();
        Long lastAttack = mahoragaLastAttackMs.get(mahoraga.getUniqueId());
        if (lastAttack == null) lastAttack = 0L;
        if (now - lastAttack < MAHORAGA_ATTACK_COOLDOWN_MS) return;

        mahoragaLastAttackMs.put(mahoraga.getUniqueId(), now);

        int cycle = mahoragaAttackCycle.getOrDefault(mahoraga.getUniqueId(), 0);
        double adaptMult = 1.0 + instance.adaptationStacks() * 0.08;

        Player damageSource = Bukkit.getPlayer(ownerUuid);

        switch (cycle) {
            case 0 -> {
                model.playAnimation(mahoraga, "attack");
                target.damage(MAHORAGA_NORMAL_DAMAGE * adaptMult, damageSource);
                mahoraga.getWorld().playSound(mahoraga.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 1.0f, 0.8f);
                mahoragaAttackCycle.put(mahoraga.getUniqueId(), 1);
            }
            case 1 -> {
                model.playAnimation(mahoraga, "attack2");
                target.damage(MAHORAGA_NORMAL_DAMAGE * adaptMult, damageSource);
                mahoraga.getWorld().playSound(mahoraga.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 1.0f, 1.0f);
                mahoragaAttackCycle.put(mahoraga.getUniqueId(), 2);
            }
            case 2 -> {
                model.playAnimation(mahoraga, "uppercut");
                target.damage(MAHORAGA_UPPERCUT_DAMAGE * adaptMult, damageSource);
                target.setVelocity(target.getVelocity().add(new Vector(0, MAHORAGA_UPPERCUT_LAUNCH_Y, 0)));
                mahoraga.getWorld().playSound(mahoraga.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 1.2f, 1.4f);

                final LivingEntity finalTarget = target;
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (!mahoraga.isValid() || !finalTarget.isValid()) return;

                    // Teleport above target
                    Location above = finalTarget.getLocation().clone().add(0, 2, 0);
                    above.setYaw(mahoraga.getLocation().getYaw());
                    teleportWithYaw(mahoraga, above);

                    model.playAnimation(mahoraga, "downslam");
                    finalTarget.damage(MAHORAGA_DOWNSLAM_DAMAGE * adaptMult, damageSource);
                    finalTarget.setFallDistance(0);

                    Vector kb = finalTarget.getLocation().toVector()
                            .subtract(mahoraga.getLocation().toVector())
                            .normalize().multiply(MAHORAGA_DOWNSLAM_KNOCKBACK);
                    kb.setY(0.3);
                    finalTarget.setVelocity(kb);

                    Location impactLoc = finalTarget.getLocation();
                    mahoraga.getWorld().spawnParticle(Particle.EXPLOSION, impactLoc, 3, 0.5, 0.2, 0.5, 0);
                    mahoraga.getWorld().spawnParticle(Particle.CLOUD, impactLoc, 30, 1.0, 0.2, 1.0, 0.02);
                    mahoraga.getWorld().playSound(impactLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.7f);
                }, MAHORAGA_DOWNSLAM_DELAY_TICKS);

                mahoragaAttackCycle.put(mahoraga.getUniqueId(), 3);
            }
            case 3 -> {
                // Adaptation phase
                if (instance.lastPhenomenonType() != null && instance.consecutiveSameHits() >= 3) {
                    instance.addAdaptationStack();

                    model.playAnimation(mahoraga, "adapt");
                    mahoraga.getWorld().spawnParticle(Particle.END_ROD,
                            mahoraga.getLocation().add(0, 3.5, 0), 40, 0.3, 0.3, 0.3, 0.02);
                    mahoraga.getWorld().playSound(mahoraga.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 0.6f);

                    for (Player nearby : mahoraga.getWorld().getPlayers()) {
                        if (nearby.getLocation().distance(mahoraga.getLocation()) <= 30) {
                            nearby.sendMessage(plugin.cfg().prefix() +
                                    instance.currentTier().displayName() + " §7— Mahoraga adapts! §8(" +
                                    (int)(instance.adaptationReduction() * 100) + "% reduction)");
                        }
                    }
                }
                mahoragaAttackCycle.put(mahoraga.getUniqueId(), 0);
            }
        }
    }

    // ========== Tick ==========

    private void tick() {
        long now = System.currentTimeMillis();

        // Tick active (friendly) shikigami
        for (Map.Entry<UUID, ShikigamiInstance> entry : activeShikigami.entrySet()) {
            UUID ownerUuid = entry.getKey();
            ShikigamiInstance instance = entry.getValue();

            Player owner = Bukkit.getPlayer(ownerUuid);
            if (owner == null || !owner.isOnline()) continue;

            // ArmorStand-based shikigami (legacy, not used by Mahoraga anymore)
            if (instance.type().usesArmorStandModel()) {
                tickMahoragaArmorStand(ownerUuid, instance, false);
                continue;
            }

            // Rabbit Escape swarm
            if (instance.type() == ShikigamiType.RABBIT_ESCAPE) {
                tickRabbitSwarm(owner, instance);
                continue;
            }

            // Divine Dogs pair
            if (instance.type().isPair()) {
                tickDivineDogsPair(owner, instance);
                continue;
            }

            // Normal mob shikigami
            if (instance.entityUuid() == null) continue;
            Entity entity = Bukkit.getEntity(instance.entityUuid());
            if (entity == null || !entity.isValid()) {
                removeActiveShikigami(ownerUuid, true);
                continue;
            }
            if (!(entity instanceof LivingEntity le)) continue;
            if (le.isDead() || le.getHealth() <= 0) {
                removeActiveShikigami(ownerUuid, true);
                continue;
            }

            if (le.getLocation().distance(owner.getLocation()) > SHIKIGAMI_HARD_LIMIT) {
                Location beside = snapToGround(owner.getLocation().clone().add(1.5, 0, 1.5));
                le.teleport(beside);
                continue;
            }

            // Dispatch per-shikigami friendly AI
            switch (instance.type()) {
                case NUE, NUE_TOTALITY -> tickNue(owner, le, instance, false);
                case TOAD -> tickToad(owner, le, instance, false);
                case GREAT_SERPENT -> tickGreatSerpent(owner, le, instance, false);
                case MAX_ELEPHANT -> tickMaxElephant(owner, le, instance, false);
                case ROUND_DEER -> tickRoundDeer(owner, le, instance);
                case PIERCING_OX -> tickPiercingOx(owner, le, instance, false);
                case MAHORAGA -> tickMahoragaMob(ownerUuid, le, instance, false);
                default -> {
                    // Generic follow
                    if (le.getLocation().distance(owner.getLocation()) > SHIKIGAMI_FOLLOW_DISTANCE) {
                        if (le instanceof Mob mob) {
                            mob.getPathfinder().moveTo(owner.getLocation(), 1.2);
                        }
                    }
                }
            }
        }

        // Tick ritual entities
        for (Map.Entry<UUID, TenShadowsProfile> entry : profiles.entrySet()) {
            UUID ownerUuid = entry.getKey();
            TenShadowsProfile prof = entry.getValue();

            if (!prof.ritualActive) continue;
            if (prof.ritualEntityUuid == null) continue;

            Player owner = Bukkit.getPlayer(ownerUuid);
            if (owner == null || !owner.isOnline()) {
                cancelRitualInternal(ownerUuid);
                continue;
            }

            ShikigamiType ritualType = ShikigamiType.from(prof.ritualTargetId);

            // ArmorStand-based ritual (legacy)
            if (ritualType != null && ritualType.usesArmorStandModel()) {
                ShikigamiInstance ritualInstance = new ShikigamiInstance(ritualType, ownerUuid);
                ritualInstance.setEntityUuid(parseUuid(prof.ritualEntityUuid));
                tickMahoragaArmorStand(ownerUuid, ritualInstance, true);

                BossBar bar = ritualBossBars.get(ownerUuid);
                if (bar != null && prof.armorStandMaxHealth > 0) {
                    double pct = prof.armorStandHealth / prof.armorStandMaxHealth;
                    bar.setProgress(Math.max(0, Math.min(1, pct)));
                }
                continue;
            }

            // Normal mob ritual
            try {
                UUID ritualEntityId = UUID.fromString(prof.ritualEntityUuid);
                Entity entity = Bukkit.getEntity(ritualEntityId);

                if (entity == null || !entity.isValid() || (entity instanceof LivingEntity le && le.isDead())) {
                    if (ritualType != null) {
                        onRitualMobDeath(ownerUuid, ritualType);
                    }
                    continue;
                }

                if (entity instanceof LivingEntity le) {
                    BossBar bar = ritualBossBars.get(ownerUuid);
                    if (bar != null) {
                        double pct = le.getHealth() / le.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
                        bar.setProgress(Math.max(0, Math.min(1, pct)));
                    }

                    // Dispatch per-shikigami HOSTILE ritual AI
                    if (ritualType != null) {
                        switch (ritualType) {
                            case NUE -> tickNue(owner, le, getRitualInstance(ownerUuid, ritualType, entity.getUniqueId()), true);
                            case TOAD -> tickToad(owner, le, getRitualInstance(ownerUuid, ritualType, entity.getUniqueId()), true);
                            case GREAT_SERPENT -> tickGreatSerpent(owner, le, getRitualInstance(ownerUuid, ritualType, entity.getUniqueId()), true);
                            case MAX_ELEPHANT -> tickMaxElephant(owner, le, getRitualInstance(ownerUuid, ritualType, entity.getUniqueId()), true);
                            case ROUND_DEER -> {
                                // Deer ritual: heals itself, making it a DPS check
                                tickRitualRoundDeer(owner, le);
                            }
                            case PIERCING_OX -> tickPiercingOx(owner, le, getRitualInstance(ownerUuid, ritualType, entity.getUniqueId()), true);
                            case MAHORAGA -> tickMahoragaMob(ownerUuid, le, getRitualInstance(ownerUuid, ritualType, entity.getUniqueId()), true);
                            default -> {
                                if (le instanceof Mob mob) mob.setTarget(owner);
                            }
                        }
                    } else {
                        if (le instanceof Mob mob) mob.setTarget(owner);
                    }
                }
            } catch (Exception ignored) {}
        }
    }

    /** Get or create a ShikigamiInstance for the ritual mob (stateful AI) */
    private final Map<UUID, ShikigamiInstance> ritualInstances = new ConcurrentHashMap<>();

    private ShikigamiInstance getRitualInstance(UUID ownerUuid, ShikigamiType type, UUID entityUuid) {
        return ritualInstances.computeIfAbsent(ownerUuid, k -> {
            ShikigamiInstance inst = new ShikigamiInstance(type, ownerUuid);
            inst.setEntityUuid(entityUuid);
            return inst;
        });
    }

    // ========== Rabbit Escape Tick ==========

    private void tickRabbitSwarm(Player owner, ShikigamiInstance instance) {
        Location center = owner.getLocation();
        int alive = 0;

        for (int i = 0; i < instance.swarmEntityUuids().size(); i++) {
            UUID rabbitId = instance.swarmEntityUuids().get(i);
            Entity e = Bukkit.getEntity(rabbitId);
            if (e == null || !e.isValid()) continue;
            alive++;

            // Form protective wall around owner
            double angle = (Math.PI * 2.0) * (i / (double) instance.swarmEntityUuids().size());
            double radius = RABBIT_WALL_RADIUS;
            Location targetLoc = center.clone().add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
            targetLoc = snapToGround(targetLoc);

            double dist = e.getLocation().distance(targetLoc);
            if (dist > 1.5) {
                if (e instanceof Mob mob) {
                    mob.getPathfinder().moveTo(targetLoc, 1.8);
                }
            }
        }

        // Maintain speed boost
        if (alive > 0 && !owner.hasPotionEffect(PotionEffectType.SPEED)) {
            owner.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 2, false, false, true));
        }

        // If all rabbits somehow die (shouldn't happen since invulnerable), dismiss
        if (alive == 0) {
            removeActiveShikigami(owner.getUniqueId(), false);
        }
    }

    // ========== Divine Dogs Pair Tick ==========

    private void tickDivineDogsPair(Player owner, ShikigamiInstance instance) {
        Entity dog1 = instance.entityUuid() != null ? Bukkit.getEntity(instance.entityUuid()) : null;
        Entity dog2 = instance.secondEntityUuid() != null ? Bukkit.getEntity(instance.secondEntityUuid()) : null;

        boolean dog1Alive = dog1 != null && dog1.isValid() && (!(dog1 instanceof LivingEntity le) || !le.isDead());
        boolean dog2Alive = dog2 != null && dog2.isValid() && (!(dog2 instanceof LivingEntity le) || !le.isDead());

        if (!dog1Alive && !dog2Alive) {
            removeActiveShikigami(owner.getUniqueId(), true);
            return;
        }

        // Both dogs: follow on each side, attack together
        Vector right = getPerpendicularRight(owner);

        if (dog1Alive && dog1 instanceof LivingEntity le1) {
            Location leftSide = owner.getLocation().clone().add(right.clone().multiply(-DOG_SIDE_OFFSET));
            if (le1.getLocation().distance(owner.getLocation()) > SHIKIGAMI_HARD_LIMIT) {
                le1.teleport(snapToGround(leftSide));
            } else if (le1.getLocation().distance(leftSide) > 2.0 && le1 instanceof Mob mob) {
                LivingEntity target = findNearestHostile(owner, le1.getLocation(), 12.0);
                if (target != null) {
                    mob.setTarget(target);
                } else {
                    mob.getPathfinder().moveTo(leftSide, 1.2);
                }
            }
        }

        if (dog2Alive && dog2 instanceof LivingEntity le2) {
            Location rightSide = owner.getLocation().clone().add(right.clone().multiply(DOG_SIDE_OFFSET));
            if (le2.getLocation().distance(owner.getLocation()) > SHIKIGAMI_HARD_LIMIT) {
                le2.teleport(snapToGround(rightSide));
            } else if (le2.getLocation().distance(rightSide) > 2.0 && le2 instanceof Mob mob) {
                LivingEntity target = findNearestHostile(owner, le2.getLocation(), 12.0);
                if (target != null) {
                    mob.setTarget(target);
                } else {
                    mob.getPathfinder().moveTo(rightSide, 1.2);
                }
            }
        }
    }

    // ========== Nue AI ==========

    private void tickNue(Player owner, LivingEntity nue, ShikigamiInstance inst, boolean hostile) {
        if (!(nue instanceof Mob mob)) return;

        // Fire resistance so Nue doesn't burn in daylight (important for ritual)
        if (!nue.hasPotionEffect(PotionEffectType.FIRE_RESISTANCE)) {
            nue.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 100, 0, false, false, false));
        }

        LivingEntity target = hostile ? owner : findNearestHostile(owner, nue.getLocation(), 25.0);
        if (target == null) {
            if (!hostile) {
                // Glide assist: give owner slow falling while Nue is summoned
                if (!owner.hasPotionEffect(PotionEffectType.SLOW_FALLING)) {
                    owner.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 40, 0, false, false, true));
                }
            }
            return;
        }

        mob.setTarget(target);
        double dist = nue.getLocation().distance(target.getLocation());
        long nowMs = System.currentTimeMillis();

        // Attack 1: Wing Attack — swoop DOWN toward target
        if (dist <= 8.0 && nowMs - inst.lastAttackMs() > 1500L) {
            inst.setLastAttackMs(nowMs);
            model.playAnimation(nue, "attack");
            // Swoop: move Nue down toward the target's head
            Location swoopLoc = target.getLocation().clone().add(0, 2, 0);
            nue.teleport(swoopLoc);
            target.damage(NUE_WING_DAMAGE, owner);
            // Knock target away from Nue
            Vector knock = target.getLocation().toVector()
                    .subtract(nue.getLocation().toVector())
                    .setY(0).normalize().multiply(1.2).setY(0.5);
            target.setVelocity(target.getVelocity().add(knock));
            nue.getWorld().playSound(nue.getLocation(), Sound.ENTITY_PHANTOM_SWOOP, 1.0f, 1.0f);
        }

        // Attack 2: Lightning Strike (faster now — NUE_LIGHTNING_COOLDOWN_MS = 4s)
        if (nowMs - inst.nueLastLightningMs() > NUE_LIGHTNING_COOLDOWN_MS) {
            inst.setNueLastLightningMs(nowMs);
            Location lightningLoc = target.getLocation().clone().add(
                (Math.random() - 0.5) * 3, 0, (Math.random() - 0.5) * 3);
            nue.getWorld().strikeLightningEffect(lightningLoc);
            double radius = inst.type() == ShikigamiType.NUE_TOTALITY ? NUE_TOTALITY_LIGHTNING_RADIUS : NUE_LIGHTNING_RADIUS;
            for (Entity nearby : nue.getWorld().getNearbyEntities(target.getLocation(), radius, radius, radius)) {
                if (!(nearby instanceof LivingEntity le)) continue;
                if (nearby.equals(nue)) continue;
                if (hostile && nearby.equals(owner)) {
                    le.damage(NUE_LIGHTNING_DAMAGE, owner);
                } else if (!hostile && !nearby.equals(owner) && !isShikigamiEntity(nearby)) {
                    le.damage(NUE_LIGHTNING_DAMAGE, owner);
                }
            }
            nue.getWorld().playSound(nue.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);
        }

        // Passive: slow falling for owner while Nue is summoned
        if (!hostile && !owner.hasPotionEffect(PotionEffectType.SLOW_FALLING)) {
            owner.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 40, 0, false, false, true));
        }
    }

    // ========== Toad AI ==========

    private void tickToad(Player owner, LivingEntity toad, ShikigamiInstance inst, boolean hostile) {
        long now = System.currentTimeMillis();
        TenShadowsProfile prof = getProfile(owner.getUniqueId());

        if (hostile) {
            // Ritual: Toad pulls owner toward it periodically
            if (now - inst.toadLastTongueMs() >= TOAD_TONGUE_COOLDOWN_MS) {
                inst.setToadLastTongueMs(now);
                double dist = toad.getLocation().distance(owner.getLocation());
                if (dist <= TOAD_TONGUE_RANGE) {
                    Vector dir = toad.getLocation().toVector().subtract(owner.getLocation().toVector()).normalize();
                    owner.setVelocity(dir.multiply(1.8).setY(0.4));
                    spawnTongueParticles(owner.getLocation(), toad.getLocation());
                    toad.getWorld().playSound(toad.getLocation(), Sound.ENTITY_FROG_TONGUE, 1.0f, 0.8f);
                }
            }
            if (toad instanceof Mob mob) mob.setTarget(owner);
            return;
        }

        // Friendly: stay at owner's side
        Vector right = getPerpendicularRight(owner);
        Location sideLoc = owner.getLocation().clone().add(right.clone().multiply(DOG_SIDE_OFFSET));
        sideLoc = snapToGround(sideLoc);

        if (toad.getLocation().distance(owner.getLocation()) > SHIKIGAMI_HARD_LIMIT) {
            toad.teleport(sideLoc);
        } else if (toad.getLocation().distance(sideLoc) > 2.5 && toad instanceof Mob mob) {
            mob.getPathfinder().moveTo(sideLoc, 1.2);
        }

        // Tongue pull: pull whoever the player last hit
        if (now - inst.toadLastTongueMs() >= TOAD_TONGUE_COOLDOWN_MS) {
            UUID pullTarget = prof.toadPullTargetUuid;
            if (pullTarget != null) {
                Entity pulled = Bukkit.getEntity(pullTarget);
                if (pulled instanceof LivingEntity le && !le.isDead() && le.getLocation().distance(toad.getLocation()) <= TOAD_TONGUE_RANGE) {
                    inst.setToadLastTongueMs(now);
                    Vector dir = owner.getLocation().toVector().subtract(le.getLocation().toVector()).normalize();
                    le.setVelocity(dir.multiply(1.8).setY(0.4));
                    spawnTongueParticles(le.getLocation(), toad.getLocation());
                    toad.getWorld().playSound(toad.getLocation(), Sound.ENTITY_FROG_TONGUE, 1.0f, 1.0f);
                    prof.toadPullTargetUuid = null; // consumed
                }
            }
        }
    }

    private void spawnTongueParticles(Location from, Location to) {
        World w = from.getWorld();
        if (w == null) return;
        Vector dir = to.toVector().subtract(from.toVector());
        double length = dir.length();
        if (length < 0.01) return;
        dir.normalize();
        for (double d = 0; d <= length; d += 0.5) {
            Location point = from.clone().add(dir.clone().multiply(d));
            w.spawnParticle(Particle.SLIME, point, 1, 0.1, 0.1, 0.1, 0);
        }
    }

    // ========== Great Serpent AI ==========

    private void tickGreatSerpent(Player owner, LivingEntity serpent, ShikigamiInstance inst, boolean hostile) {
        long now = System.currentTimeMillis();
        TenShadowsProfile prof = getProfile(owner.getUniqueId());

        // Friendly: only attack what player is targeting
        LivingEntity target;
        if (hostile) {
            target = owner;
        } else {
            // Use the player's designated target (set when they hit an entity)
            target = null;
            if (prof.serpentTargetUuid != null) {
                Entity e = Bukkit.getEntity(prof.serpentTargetUuid);
                if (e instanceof LivingEntity le && !le.isDead() && le.getLocation().distance(serpent.getLocation()) <= 25.0) {
                    target = le;
                } else {
                    prof.serpentTargetUuid = null; // stale/out of range
                }
            }
        }

        if (target == null) {
            if (!hostile) {
                if (serpent.getLocation().distance(owner.getLocation()) > SHIKIGAMI_FOLLOW_DISTANCE) {
                    if (serpent instanceof Mob mob) mob.getPathfinder().moveTo(owner.getLocation(), 1.0);
                }
            }
            return;
        }

        if (serpent instanceof Mob mob) mob.setTarget(target);

        // Prevent fall damage for serpent
        serpent.setFallDistance(0f);

        // Shake phase — Ravager also moves upward during the shake
        if (inst.serpentGrabbedTarget() != null && inst.serpentShakeTicksLeft() > 0) {
            Entity grabbed = Bukkit.getEntity(inst.serpentGrabbedTarget());
            if (grabbed instanceof LivingEntity le && !le.isDead()) {
                le.damage(SERPENT_SHAKE_DAMAGE_PER_TICK, owner);
                // Random shake velocity
                Random rng = new Random();
                le.setVelocity(new Vector((rng.nextDouble() - 0.5) * 0.8, 0.3, (rng.nextDouble() - 0.5) * 0.8));
                serpent.getWorld().spawnParticle(Particle.CRIT, le.getLocation(), 3, 0.3, 0.2, 0.3, 0);
                // Move serpent upward to visually track the grabbed target
                Location serpentRise = serpent.getLocation().clone().add(0, 0.15, 0);
                serpent.teleport(serpentRise);
            } else {
                inst.setSerpentGrabbedTarget(null);
                inst.setSerpentShakeTicksLeft(0);
            }
            inst.decrementSerpentShakeTicks();
            return;
        }

        // Release phase done
        if (inst.serpentGrabbedTarget() != null && inst.serpentShakeTicksLeft() <= 0) {
            Entity grabbed = Bukkit.getEntity(inst.serpentGrabbedTarget());
            if (grabbed instanceof LivingEntity le) {
                le.removePotionEffect(PotionEffectType.LEVITATION);
                le.setVelocity(new Vector(0, -0.5, 0));
            }
            inst.setSerpentGrabbedTarget(null);
        }

        // Grab attack
        if (now - inst.serpentLastGrabMs() > SERPENT_GRAB_COOLDOWN_MS) {
            double dist = serpent.getLocation().distance(target.getLocation());
            if (dist <= 10.0) {
                inst.setSerpentLastGrabMs(now);

                // Emerge from ground effect
                Location targetLoc = target.getLocation();
                serpent.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE, targetLoc, 40, 0.5, 0.5, 0.5, 0,
                    serpent.getWorld().getBlockAt(targetLoc).getBlockData());
                serpent.getWorld().playSound(targetLoc, Sound.BLOCK_GRAVEL_BREAK, 1.0f, 0.5f);

                // Teleport serpent (Ravager) underground to rise effect
                Location belowGround = targetLoc.clone().add(0, -1, 0);
                serpent.teleport(belowGround);

                // Grab target — teleport above + levitation
                Location above = targetLoc.clone().add(0, 4, 0);
                target.teleport(above);
                target.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, SERPENT_SHAKE_DURATION_TICKS, 1, false, false, false));
                target.damage(SERPENT_INITIAL_GRAB_DAMAGE, owner);

                inst.setSerpentGrabbedTarget(target.getUniqueId());
                inst.setSerpentShakeTicksLeft(SERPENT_SHAKE_DURATION_TICKS);

                serpent.getWorld().playSound(targetLoc, Sound.ENTITY_RAVAGER_ROAR, 1.0f, 0.5f);
            }
        }
    }

    // ========== Max Elephant AI ==========

    private void tickMaxElephant(Player owner, LivingEntity elephant, ShikigamiInstance inst, boolean hostile) {
        long now = System.currentTimeMillis();

        // Prevent fall damage
        elephant.setFallDistance(0f);

        if (!hostile) {
            // Friendly mode: elephant stands stationary, rotates to face where player is looking.
            // Water spew is continuous every tick (via block placement on every short interval).
            Vector horizLook = owner.getLocation().getDirection().clone().setY(0).normalize();
            float yaw = dirToYaw(horizLook);
            Location el = elephant.getLocation().clone();
            el.setYaw(yaw);
            teleportWithYaw(elephant, el);

            // Continuous water spew in direction player is looking (refresh every 10 ticks = 500ms)
            if (now - inst.elephantLastCrushMs() > 500L) {
                inst.setElephantLastCrushMs(now);
                spawnElephantWaterSpew(owner, inst);
            }
            return;
        }

        // Hostile (ritual): crush from above
        LivingEntity target = owner;
        if (elephant instanceof Mob mob) mob.setTarget(target);

        if (now - inst.elephantLastCrushMs() < ELEPHANT_CRUSH_COOLDOWN_MS) return;
        inst.setElephantLastCrushMs(now);

        // Spawn high above target then fall
        Random rng = new Random();
        int height = ELEPHANT_CRUSH_MIN_HEIGHT + rng.nextInt(ELEPHANT_CRUSH_MAX_HEIGHT - ELEPHANT_CRUSH_MIN_HEIGHT + 1);
        double damage = ELEPHANT_MIN_DAMAGE + ((double)height / ELEPHANT_CRUSH_MAX_HEIGHT) * (ELEPHANT_MAX_DAMAGE - ELEPHANT_MIN_DAMAGE);

        Location above = target.getLocation().clone().add(
            (rng.nextDouble() - 0.5) * 4, height, (rng.nextDouble() - 0.5) * 4);
        elephant.teleport(above);
        elephant.setVelocity(new Vector(0, -2.0, 0));

        final LivingEntity finalTarget = target;
        final double finalDamage = damage;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!elephant.isValid()) return;
            elephant.setFallDistance(0f); // no fall damage on landing
            // Check if landed near target
            double dist = elephant.getLocation().distance(finalTarget.getLocation());
            if (dist <= 5.0) {
                finalTarget.damage(finalDamage, owner);
                finalTarget.setVelocity(new Vector(
                    (rng.nextDouble() - 0.5) * 2, 1.5, (rng.nextDouble() - 0.5) * 2));
                elephant.getWorld().spawnParticle(Particle.EXPLOSION, elephant.getLocation(), 5, 1.0, 0.5, 1.0, 0);
                elephant.getWorld().playSound(elephant.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.6f);
            }
        }, 40L);
    }

    private void spawnElephantWaterSpew(Player owner, ShikigamiInstance inst) {
        Location origin = owner.getLocation().clone().add(0, 1, 0);
        Vector dir = owner.getLocation().getDirection().clone().setY(0).normalize();
        World w = owner.getWorld();
        if (w == null) return;

        // First, clear old water blocks that are no longer in the current spray direction
        inst.placedWaterBlocks().removeIf(loc -> {
            Block b = loc.getBlock();
            if (b.getType() == Material.WATER) {
                b.setType(Material.AIR);
            }
            return true;
        });

        // Place fresh water stream
        for (int i = 1; i <= 12; i++) {
            Location waterLoc = origin.clone().add(dir.clone().multiply(i));
            Block b = waterLoc.getBlock();
            if (b.getType() == Material.AIR) {
                b.setType(Material.WATER);
                inst.placedWaterBlocks().add(waterLoc.clone());
            }
        }
    }

    // ========== Round Deer AI ==========

    private void tickRoundDeer(Player owner, LivingEntity deer, ShikigamiInstance inst) {
        long now = System.currentTimeMillis();
        if (now - inst.deerLastHealMs() < DEER_HEAL_INTERVAL_MS) return;
        inst.setDeerLastHealMs(now);

        Location deerLoc = deer.getLocation();
        World w = deerLoc.getWorld();
        if (w == null) return;

        // Spawn healing particles circle
        for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 12) {
            Location particleLoc = deerLoc.clone().add(
                Math.cos(angle) * DEER_HEAL_RADIUS, 0.5, Math.sin(angle) * DEER_HEAL_RADIUS);
            w.spawnParticle(Particle.HAPPY_VILLAGER, particleLoc, 1, 0.1, 0.1, 0.1, 0);
        }

        // Regeneration for players in radius
        for (Player p : w.getPlayers()) {
            if (p.getLocation().distance(deerLoc) <= DEER_HEAL_RADIUS) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 40, 1, false, false, true));
            }
        }

        // Follow owner
        if (deer.getLocation().distance(owner.getLocation()) > SHIKIGAMI_FOLLOW_DISTANCE + 2) {
            if (deer instanceof Mob mob) mob.getPathfinder().moveTo(owner.getLocation(), 1.0);
        }
    }

    private void tickRitualRoundDeer(Player owner, LivingEntity deer) {
        // Ritual deer heals itself — making it a DPS check
        if (deer.getHealth() < deer.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() - 2) {
            deer.setHealth(Math.min(deer.getHealth() + 1.5,
                deer.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()));
            deer.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, deer.getLocation().add(0, 1.5, 0),
                5, 0.3, 0.5, 0.3, 0);
        }
        if (deer instanceof Mob mob) mob.setTarget(owner);
    }

    // ========== Piercing Ox AI ==========

    private void tickPiercingOx(Player owner, LivingEntity ox, ShikigamiInstance inst, boolean hostile) {
        long now = System.currentTimeMillis();

        if (now - inst.oxLastChargeMs() < OX_CHARGE_COOLDOWN_MS) {
            // Continue charging if in progress
            if (inst.isOxCharging() && inst.oxSpawnPoint() != null) {
                continueOxCharge(owner, ox, inst, hostile);
            }
            return;
        }

        // Start a new charge
        inst.setOxLastChargeMs(now);
        inst.setOxCharging(true);

        if (hostile) {
            // Ritual: charge from a random angle
            double angle = Math.random() * Math.PI * 2;
            double dist = 15.0 + Math.random() * 10;
            Location spawnPt = owner.getLocation().clone().add(
                Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
            spawnPt = snapToGround(spawnPt);
            ox.teleport(spawnPt);
            inst.setOxSpawnPoint(spawnPt.clone());

            // Face toward the owner
            Vector chargeDir = owner.getLocation().toVector().subtract(spawnPt.toVector()).setY(0).normalize();
            spawnPt.setYaw(dirToYaw(chargeDir));
            ox.teleport(spawnPt);
            ox.setVelocity(chargeDir.multiply(OX_CHARGE_SPEED * 2));

            ox.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE, ox.getLocation(), 20, 0.5, 0.3, 0.5, 0,
                ox.getWorld().getBlockAt(ox.getLocation()).getBlockData());
            ox.getWorld().playSound(ox.getLocation(), Sound.ENTITY_RAVAGER_ATTACK, 1.0f, 0.8f);
        } else {
            // Friendly: charge in the direction the PLAYER is LOOKING (horizontal)
            inst.setOxSpawnPoint(ox.getLocation().clone());
            Vector chargeDir = owner.getLocation().getDirection().clone().setY(0).normalize();
            // Position ox directly in front of the player facing the same direction
            Location startLoc = owner.getLocation().clone().add(chargeDir.clone().multiply(-2));
            startLoc = snapToGround(startLoc);
            startLoc.setYaw(dirToYaw(chargeDir));
            ox.teleport(startLoc);
            inst.setOxSpawnPoint(startLoc.clone());
            ox.setVelocity(chargeDir.multiply(OX_CHARGE_SPEED * 2.5));
            ox.getWorld().playSound(ox.getLocation(), Sound.ENTITY_RAVAGER_ATTACK, 1.0f, 1.0f);
        }
    }

    private void continueOxCharge(Player owner, LivingEntity ox, ShikigamiInstance inst, boolean hostile) {
        if (inst.oxSpawnPoint() == null) { inst.setOxCharging(false); return; }
        double traveled = ox.getLocation().distance(inst.oxSpawnPoint());

        // Check hit with target
        LivingEntity target = hostile ? owner : null;
        if (!hostile) target = findNearestHostile(owner, ox.getLocation(), 2.0);

        if (target != null) {
            double hitDist = ox.getLocation().distance(target.getLocation());
            if (hitDist <= 2.5) {
                double dmg = OX_MIN_DAMAGE + Math.min(traveled / OX_MAX_CHARGE_DISTANCE, 1.0) * (OX_MAX_DAMAGE - OX_MIN_DAMAGE);
                target.damage(dmg, owner);
                target.setVelocity(ox.getVelocity().clone().setY(0.8));
                ox.getWorld().spawnParticle(Particle.EXPLOSION, ox.getLocation(), 3, 0.5, 0.3, 0.5, 0);
                ox.getWorld().playSound(ox.getLocation(), Sound.ENTITY_RAVAGER_STUNNED, 1.0f, 0.7f);
                inst.setOxCharging(false);
            }
        }

        if (traveled > OX_MAX_CHARGE_DISTANCE) {
            inst.setOxCharging(false);
        }
    }

    // ========== Mahoraga Iron Golem AI ==========

    private void tickMahoragaMob(UUID ownerUuid, LivingEntity mahoraga, ShikigamiInstance inst, boolean isRitual) {
        Player owner = Bukkit.getPlayer(ownerUuid);
        if (owner == null || !owner.isOnline()) return;

        Location ml = mahoraga.getLocation();
        LivingEntity target = isRitual ? owner : findNearestHostile(owner, ml, 20.0);

        if (target == null) {
            if (!isRitual) {
                // Follow owner
                double dist = ml.distance(owner.getLocation());
                if (dist > MAHORAGA_HARD_LIMIT) {
                    mahoraga.teleport(snapToGround(owner.getLocation().clone().add(1.5, 0, 1.5)));
                } else if (dist > MAHORAGA_FOLLOW_DISTANCE && mahoraga instanceof Mob mob) {
                    mob.getPathfinder().moveTo(owner.getLocation(), MAHORAGA_FOLLOW_SPEED);
                }
                // Update boss bar
                updateMahoragaBossBar(ownerUuid, inst);
            }
            return;
        }

        if (mahoraga instanceof Mob mob) mob.setTarget(target);

        double dist = ml.distance(target.getLocation());
        if (dist > MAHORAGA_ATTACK_RANGE && mahoraga instanceof Mob mob) {
            mob.getPathfinder().moveTo(target.getLocation(), MAHORAGA_CHASE_SPEED);
        }

        if (dist <= MAHORAGA_ATTACK_RANGE) {
            tickMahoragaMobAttack(mahoraga, target, ownerUuid, inst, isRitual);
        }

        if (!isRitual) updateMahoragaBossBar(ownerUuid, inst);
    }

    private void tickMahoragaMobAttack(LivingEntity mahoraga, LivingEntity target, UUID ownerUuid, ShikigamiInstance inst, boolean isRitual) {
        long now = System.currentTimeMillis();
        if (now - inst.lastAttackMs() < MAHORAGA_ATTACK_COOLDOWN_MS) return;
        inst.setLastAttackMs(now);

        Player damageSource = Bukkit.getPlayer(ownerUuid);
        double adaptMult = 1.0 + inst.wheelSpins() * 0.08;
        int cycle = inst.attackCycle();

        // Special combo (Uppercut+Downslam) only fires every MAHORAGA_SPECIAL_COMBO_INTERVAL attacks
        boolean isSpecialCombo = (cycle == MAHORAGA_SPECIAL_COMBO_INTERVAL);

        if (isSpecialCombo) {
            // Uppercut — launches ~200 blocks high
            model.playAnimation(mahoraga, "uppercut");
            // Randomize direction slightly
            double xOffset = (Math.random() - 0.5) * 0.3;
            double zOffset = (Math.random() - 0.5) * 0.3;
            target.damage(MAHORAGA_UPPERCUT_DAMAGE * adaptMult, damageSource);
            target.setVelocity(target.getVelocity().add(new Vector(xOffset, MAHORAGA_UPPERCUT_LAUNCH_Y, zOffset)));
            mahoraga.getWorld().playSound(mahoraga.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 1.2f, 1.4f);

            final LivingEntity finalTarget = target;
            final double fAdaptMult = adaptMult;
            // After MAHORAGA_DOWNSLAM_DELAY_TICKS ticks (~apex time), teleport Mahoraga just above target
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!mahoraga.isValid() || !finalTarget.isValid()) return;

                // Teleport Mahoraga to just above target's current position (near apex)
                Location targetNow = finalTarget.getLocation().clone();
                Location above = targetNow.clone().add(0, 5, 0);
                above.setYaw(mahoraga.getLocation().getYaw());
                mahoraga.teleport(above);

                model.playAnimation(mahoraga, "downslam");
                // Slam down: deal damage and apply strong downward force to target
                finalTarget.damage(MAHORAGA_DOWNSLAM_DAMAGE * fAdaptMult, damageSource);
                finalTarget.setFallDistance(0);
                finalTarget.setVelocity(new Vector(0, -MAHORAGA_DOWNSLAM_KNOCKBACK, 0));

                mahoraga.getWorld().spawnParticle(Particle.EXPLOSION, targetNow, 5, 1.0, 0.2, 1.0, 0);
                mahoraga.getWorld().spawnParticle(Particle.CLOUD, targetNow, 40, 1.5, 0.3, 1.5, 0.05);
                mahoraga.getWorld().playSound(targetNow, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.5f);

                // Teleport Mahoraga back to ground after the slam
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (mahoraga.isValid()) mahoraga.teleport(snapToGround(targetNow.clone()));
                }, 20L);
            }, MAHORAGA_DOWNSLAM_DELAY_TICKS);
            inst.setAttackCycle(0); // reset after special combo
        } else {
            // Normal attacks alternate between attack and attack2
            if (cycle % 2 == 0) {
                model.playAnimation(mahoraga, "attack");
                mahoraga.getWorld().playSound(mahoraga.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 1.0f, 0.8f);
            } else {
                model.playAnimation(mahoraga, "attack2");
                mahoraga.getWorld().playSound(mahoraga.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 1.0f, 1.0f);
            }
            target.damage(MAHORAGA_NORMAL_DAMAGE * adaptMult, damageSource);
            inst.setAttackCycle(cycle + 1);

            // Check if we should attempt wheel adaptation after each hit
            String phenomenonType = inst.lastPhenomenonType();
            if (phenomenonType != null && inst.wheelSpins() < AdaptationTier.MAX_SPINS) {
                boolean spun = inst.trySpinWheel(phenomenonType);
                if (spun) {
                    model.playAnimation(mahoraga, "adapt");
                    mahoraga.getWorld().spawnParticle(Particle.END_ROD,
                        mahoraga.getLocation().clone().add(0, 3.5, 0), 40, 0.3, 0.3, 0.3, 0.02);
                    mahoraga.getWorld().playSound(mahoraga.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 0.6f);

                    // Regenerate ~12% of max HP on adaptation
                    try {
                        double maxHp = mahoraga.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
                        double healAmount = maxHp * 0.12;
                        mahoraga.setHealth(Math.min(maxHp, mahoraga.getHealth() + healAmount));
                    } catch (Exception ignored) {}

                    String msg = plugin.cfg().prefix() + "§5Mahoraga has started adapting to §f" + phenomenonType + "§5! "
                        + inst.currentTier().displayName() + " §8(" + (int)(inst.adaptationReduction() * 100) + "% reduction)";
                    for (Player nearby : mahoraga.getWorld().getPlayers()) {
                        if (nearby.getLocation().distance(mahoraga.getLocation()) <= 50) {
                            nearby.sendMessage(msg);
                        }
                    }
                }
            }
        }
    }

    private void updateMahoragaBossBar(UUID ownerUuid, ShikigamiInstance inst) {
        BossBar bar = mahoragaBossBars.get(ownerUuid);
        if (bar == null) return;
        Entity e = inst.entityUuid() != null ? Bukkit.getEntity(inst.entityUuid()) : null;
        if (!(e instanceof LivingEntity le)) return;
        try {
            double pct = le.getHealth() / le.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
            bar.setProgress(Math.max(0, Math.min(1, pct)));
            bar.setTitle("§5§lMahoraga §8| " + inst.currentTier().displayName());
        } catch (Exception ignored) {}
    }

    /**
     * Notify that Mahoraga was hit with a specific phenomenon type.
     * Updates the adaptation tracking on the ShikigamiInstance.
     */
    public void notifyMahoragaHit(UUID ownerUuid, String phenomenon, double damage) {
        // Check active shikigami first
        ShikigamiInstance inst = activeShikigami.get(ownerUuid);
        if (inst != null && inst.type() == ShikigamiType.MAHORAGA) {
            inst.setLastPhenomenonType(phenomenon);
            return;
        }
        // Check ritual instance
        ShikigamiInstance ritualInst = ritualInstances.get(ownerUuid);
        if (ritualInst != null && ritualInst.type() == ShikigamiType.MAHORAGA) {
            ritualInst.setLastPhenomenonType(phenomenon);
        }
    }

    /** Open the shadow storage for a player */
    public void openShadowStorage(Player p) {
        if (!hasTechnique(p)) {
            p.sendMessage(plugin.cfg().prefix() + "§cYou don't have Ten Shadows.");
            return;
        }
        plugin.shadowStorage().open(p);
    }

        private void cancelRitualInternal(UUID ownerUuid) {
        TenShadowsProfile prof = getProfile(ownerUuid);
        removeEntity(parseUuid(prof.ritualEntityUuid));
        BossBar bar = ritualBossBars.remove(ownerUuid);
        if (bar != null) bar.removeAll();
        prof.ritualActive = false;
        prof.ritualTargetId = null;
        prof.ritualEntityUuid = null;
        ritualInstances.remove(ownerUuid);
    }

    // ========== Utility ==========

    public boolean isShikigamiEntity(Entity e) {
        if (e == null || !e.isValid()) return false;
        Integer v = e.getPersistentDataContainer().get(KEY_SHIKIGAMI, PersistentDataType.INTEGER);
        return v != null && v == 1;
    }

    public boolean isRitualMob(Entity e) {
        if (!isShikigamiEntity(e)) return false;
        Integer v = e.getPersistentDataContainer().get(KEY_RITUAL_MOB, PersistentDataType.INTEGER);
        return v != null && v == 1;
    }

    public UUID getShikigamiOwner(Entity e) {
        if (!isShikigamiEntity(e)) return null;
        String s = e.getPersistentDataContainer().get(KEY_SHIKIGAMI_OWNER, PersistentDataType.STRING);
        if (s == null) return null;
        try { return UUID.fromString(s); } catch (Exception ex) { return null; }
    }

    public ShikigamiType getShikigamiType(Entity e) {
        if (!isShikigamiEntity(e)) return null;
        String s = e.getPersistentDataContainer().get(KEY_SHIKIGAMI_TYPE, PersistentDataType.STRING);
        return ShikigamiType.from(s);
    }

    /** Check if an ArmorStand-based shikigami belongs to an owner */
    public boolean isArmorStandShikigami(Entity e, UUID ownerUuid) {
        if (!isShikigamiEntity(e)) return false;
        ShikigamiType type = getShikigamiType(e);
        if (type == null || !type.usesArmorStandModel()) return false;
        UUID entityOwner = getShikigamiOwner(e);
        return ownerUuid.equals(entityOwner);
    }

    public void cleanup(UUID ownerUuid) {
        removeActiveShikigami(ownerUuid, false);
        cancelRitualInternal(ownerUuid);
        rabbitWallActive.remove(ownerUuid);

        Player owner = Bukkit.getPlayer(ownerUuid);
        if (owner != null) {
            owner.removePotionEffect(PotionEffectType.SPEED);
        }
    }

    private LivingEntity findNearestHostile(Player owner, Location from, double range) {
        LivingEntity best = null;
        double bestDist = range;

        for (Entity e : from.getWorld().getNearbyEntities(from, range, range, range)) {
            if (!(e instanceof LivingEntity le)) continue;
            if (e.getUniqueId().equals(owner.getUniqueId())) continue;
            if (e instanceof ArmorStand) continue;
            if (isShikigamiEntity(e)) continue;
            if (plugin.rika() != null && plugin.rika().isRikaEntity(e)) continue;

            double dist = e.getLocation().distance(from);
            if (dist < bestDist) {
                bestDist = dist;
                best = le;
            }
        }
        return best;
    }

    private void spawnSummonParticles(Location loc) {
        World w = loc.getWorld();
        if (w == null) return;
        Particle.DustOptions dark = new Particle.DustOptions(Color.fromRGB(20, 0, 40), 2.0f);
        w.spawnParticle(Particle.DUST, loc.clone().add(0, 1, 0), 60, 0.8, 1.0, 0.8, 0, dark);
        w.spawnParticle(Particle.SMOKE, loc.clone().add(0, 0.5, 0), 30, 0.5, 0.3, 0.5, 0.02);
    }

    private void spawnRitualParticles(Location loc) {
        World w = loc.getWorld();
        if (w == null) return;
        Particle.DustOptions red = new Particle.DustOptions(Color.fromRGB(180, 0, 0), 2.5f);
        w.spawnParticle(Particle.DUST, loc.clone().add(0, 1.5, 0), 80, 1.2, 1.5, 1.2, 0, red);
        w.spawnParticle(Particle.FLAME, loc.clone().add(0, 0.5, 0), 40, 0.8, 0.3, 0.8, 0.03);
        w.spawnParticle(Particle.SOUL_FIRE_FLAME, loc.clone().add(0, 1, 0), 30, 0.6, 0.6, 0.6, 0.02);
    }

    private Location snapToGround(Location loc) {
        World w = loc.getWorld();
        if (w == null) return loc;
        int x = loc.getBlockX(); int z = loc.getBlockZ(); int startY = loc.getBlockY();
        for (int y = startY + 5; y >= Math.max(w.getMinHeight(), startY - 10); y--) {
            if (!w.getBlockAt(x, y, z).getType().isAir() && w.getBlockAt(x, y + 1, z).getType().isAir()) {
                return new Location(w, loc.getX(), y + 1.0, loc.getZ(), loc.getYaw(), loc.getPitch());
            }
        }
        return loc;
    }

    private float dirToYaw(Vector dir) {
        if (dir.lengthSquared() < 0.001) return 0;
        return (float) Math.toDegrees(Math.atan2(-dir.getX(), dir.getZ())) - 180f;
    }

    private void teleportWithYaw(Entity entity, Location loc) {
        entity.teleport(loc);
        if (entity instanceof ArmorStand as) {
            as.setRotation(loc.getYaw(), 0);
        }
        model.forceBodyYaw(entity, loc.getYaw());
    }

    private double lerpY(double currentY, double targetY) {
        double diff = targetY - currentY;
        if (Math.abs(diff) < 0.05) return targetY;
        return currentY + diff * MAHORAGA_Y_LERP;
    }

    private Vector getPerpendicularRight(Player owner) {
        Vector dir = owner.getLocation().getDirection().normalize();
        return new Vector(-dir.getZ(), 0, dir.getX()).normalize();
    }

    private UUID parseUuid(String s) {
        if (s == null) return null;
        try { return UUID.fromString(s); } catch (Exception e) { return null; }
    }
}