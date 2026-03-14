package me.axebanz.jJK;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.util.Vector;

/**
 * Contains the unique AI behavior for each shikigami type.
 * All shikigami fight back (both in rituals and when summoned).
 */
public final class ShikigamiAI {

    private final JJKCursedToolsPlugin plugin;
    private final ModelEngineBridge model;

    private static final double FOLLOW_DISTANCE = 3.5;
    private static final double ATTACK_RANGE = 3.5;
    private static final double HARD_LIMIT = 25.0;
    private static final long ATTACK_CD_MS = 1200;

    public ShikigamiAI(JJKCursedToolsPlugin plugin, ModelEngineBridge model) {
        this.plugin = plugin;
        this.model = model;
    }

    /**
     * Tick a friendly (summoned) shikigami.
     * Each type has unique behavior.
     */
    public void tickFriendly(Player owner, LivingEntity entity, ShikigamiInstance instance) {
        if (entity == null || !entity.isValid() || entity.isDead()) return;

        switch (instance.type()) {
            case DIVINE_DOGS, DIVINE_DOG_TOTALITY -> tickDivineDogs(owner, entity, instance);
            case NUE -> tickNue(owner, entity, instance);
            case TOAD -> tickToad(owner, entity, instance);
            case GREAT_SERPENT -> tickGreatSerpent(owner, entity, instance);
            case MAX_ELEPHANT -> tickMaxElephant(owner, entity, instance);
            case ROUND_DEER -> tickRoundDeer(owner, entity, instance);
            case PIERCING_OX -> tickPiercingOx(owner, entity, instance);
            case TIGER_FUNERAL -> tickTigerFuneral(owner, entity, instance);
            case MAHORAGA -> tickMahoragaFriendly(owner, entity, instance);
            default -> tickGenericFollow(owner, entity);
        }
    }

    /**
     * Tick a hostile (ritual) shikigami. They fight the owner.
     */
    public void tickHostile(Player owner, LivingEntity entity, ShikigamiInstance instance) {
        if (entity == null || !entity.isValid() || entity.isDead()) return;

        double dist = entity.getLocation().distance(owner.getLocation());

        // Chase owner
        if (entity instanceof Mob mob) {
            mob.setTarget(owner);
        }

        switch (instance.type()) {
            case MAHORAGA -> tickMahoragaHostile(owner, entity, instance, dist);
            default -> {
                // Generic hostile: chase and attack
                if (dist <= ATTACK_RANGE) {
                    genericAttack(entity, owner, 6.0);
                }
            }
        }
    }

    // ===== DIVINE DOGS =====
    // Stay by owner's side, lunge at nearby enemies, track curses

    private void tickDivineDogs(Player owner, LivingEntity entity, ShikigamiInstance instance) {
        LivingEntity target = findNearestHostile(owner, entity, 12.0);

        if (target != null) {
            double dist = entity.getLocation().distance(target.getLocation());
            if (dist <= ATTACK_RANGE) {
                double dmg = instance.type() == ShikigamiType.DIVINE_DOG_TOTALITY ? 10.0 : 6.0;
                genericAttack(entity, target, dmg);
                model.playAnimation(entity, "attack");
            } else {
                moveToward(entity, target.getLocation(), 1.4);
            }
        } else {
            followOwnerSide(owner, entity, 2.0);
        }
    }

    // ===== NUE =====
    // Flies, electric shock attacks, can carry owner, paralyzes targets

    private void tickNue(Player owner, LivingEntity entity, ShikigamiInstance instance) {
        // Nue hovers above
        Location above = owner.getLocation().clone().add(0, 3.0, 0);
        if (entity.getLocation().distance(above) > 5.0) {
            entity.teleport(above);
        }

        LivingEntity target = findNearestHostile(owner, entity, 15.0);
        if (target != null) {
            double dist = entity.getLocation().distance(target.getLocation());
            if (dist <= 5.0) {
                // Electric shock dive
                model.playAnimation(entity, "attack");
                target.damage(8.0, owner);
                // Paralysis (slowness + no jump)
                target.addPotionEffect(new org.bukkit.potion.PotionEffect(
                        org.bukkit.potion.PotionEffectType.SLOWNESS, 40, 5, false, false, true));
                target.addPotionEffect(new org.bukkit.potion.PotionEffect(
                        org.bukkit.potion.PotionEffectType.JUMP_BOOST, 40, 250, false, false, true));
                // Lightning particle at target
                target.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                        target.getLocation().add(0, 1, 0), 30, 0.4, 0.6, 0.4, 0.05);
                target.getWorld().playSound(target.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.6f, 1.4f);
            } else {
                moveToward(entity, target.getLocation().clone().add(0, 2, 0), 1.6);
            }
        }
    }

    // ===== TOAD =====
    // Pulls targets to owner, or pulls owner out of danger

    private void tickToad(Player owner, LivingEntity entity, ShikigamiInstance instance) {
        // Toad stays by owner's side and rotates with them
        followOwnerSide(owner, entity, 2.5);

        // If owner is looking at a target, pull them
        LivingEntity lookTarget = getOwnerLookTarget(owner, 18.0);
        if (lookTarget != null) {
            double dist = entity.getLocation().distance(lookTarget.getLocation());
            if (dist <= 18.0 && dist > 3.0) {
                // Tongue pull — yank target toward owner
                Vector pull = owner.getLocation().toVector().subtract(lookTarget.getLocation().toVector()).normalize().multiply(1.8);
                pull.setY(0.4);
                lookTarget.setVelocity(pull);
                entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_FROG_TONGUE, 1.0f, 0.8f);
                model.playAnimation(entity, "attack");

                // Particle tongue line
                spawnTongueLine(entity.getLocation().clone().add(0, 0.8, 0), lookTarget.getLocation().clone().add(0, 1, 0));
            }
        }
    }

    // ===== GREAT SERPENT =====
    // Surprise attack from shadows, immobilizes target

    private void tickGreatSerpent(Player owner, LivingEntity entity, ShikigamiInstance instance) {
        LivingEntity target = findNearestHostile(owner, entity, 15.0);

        if (target != null) {
            double dist = entity.getLocation().distance(target.getLocation());
            if (dist <= ATTACK_RANGE + 1.0) {
                // Constrict — immobilize and damage
                model.playAnimation(entity, "attack");
                target.damage(8.0, owner);
                target.addPotionEffect(new org.bukkit.potion.PotionEffect(
                        org.bukkit.potion.PotionEffectType.SLOWNESS, 60, 10, false, false, true));
                target.addPotionEffect(new org.bukkit.potion.PotionEffect(
                        org.bukkit.potion.PotionEffectType.JUMP_BOOST, 60, 250, false, false, true));
                target.setVelocity(new Vector(0, 0, 0));
                entity.getWorld().playSound(target.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_AMBIENT, 0.8f, 0.6f);
            } else {
                moveToward(entity, target.getLocation(), 1.5);
            }
        } else {
            // Hide in owner's shadow (stay very close)
            followOwnerSide(owner, entity, 1.5);
        }
    }

    // ===== MAX ELEPHANT =====
    // Spews water, crushes targets with weight

    private void tickMaxElephant(Player owner, LivingEntity entity, ShikigamiInstance instance) {
        LivingEntity target = findNearestHostile(owner, entity, 12.0);

        if (target != null) {
            double dist = entity.getLocation().distance(target.getLocation());
            if (dist <= 6.0) {
                // Water blast
                model.playAnimation(entity, "attack");
                Vector knockback = target.getLocation().toVector()
                        .subtract(entity.getLocation().toVector()).normalize().multiply(2.0);
                knockback.setY(0.5);
                target.setVelocity(knockback);
                target.damage(10.0, owner);

                // Water particles
                Location mid = entity.getLocation().clone().add(0, 1.5, 0);
                Vector dir = target.getLocation().toVector().subtract(mid.toVector()).normalize();
                for (double d = 0; d < dist; d += 0.5) {
                    Location p = mid.clone().add(dir.clone().multiply(d));
                    entity.getWorld().spawnParticle(Particle.SPLASH, p, 5, 0.2, 0.2, 0.2, 0);
                }
                entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_GENERIC_SPLASH, 1.0f, 0.8f);
            } else {
                moveToward(entity, target.getLocation(), 0.8);
            }
        } else {
            tickGenericFollow(owner, entity);
        }
    }

    // ===== ROUND DEER =====
    // Heals owner through reverse cursed technique

    private void tickRoundDeer(Player owner, LivingEntity entity, ShikigamiInstance instance) {
        followOwnerSide(owner, entity, 3.0);

        // Heal owner if damaged
        if (owner.getHealth() < owner.getMaxHealth() * 0.8) {
            double heal = 2.0; // 1 heart per tick cycle
            owner.setHealth(Math.min(owner.getMaxHealth(), owner.getHealth() + heal));
            // Positive energy particles
            owner.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
                    owner.getLocation().add(0, 1, 0), 8, 0.3, 0.3, 0.3, 0);
            entity.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
                    entity.getLocation().add(0, 1.5, 0), 5, 0.3, 0.3, 0.3, 0);
        }

        // Defensive: if enemy is close, emit positive energy burst that disrupts
        LivingEntity target = findNearestHostile(owner, entity, 6.0);
        if (target != null) {
            // Positive energy pulse — knockback + minor damage
            Vector kb = target.getLocation().toVector().subtract(entity.getLocation().toVector()).normalize().multiply(1.2);
            kb.setY(0.3);
            target.setVelocity(kb);
            target.damage(4.0, owner);
            entity.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
                    entity.getLocation().add(0, 1, 0), 30, 1.5, 1.0, 1.5, 0);
        }
    }

    // ===== PIERCING OX =====
    // Charges in a straight line with immense force

    private void tickPiercingOx(Player owner, LivingEntity entity, ShikigamiInstance instance) {
        LivingEntity target = findNearestHostile(owner, entity, 20.0);

        if (target != null) {
            double dist = entity.getLocation().distance(target.getLocation());
            // Charge! The further the charge distance, the more damage
            if (dist > 5.0) {
                // Build up charge — move toward fast
                Vector chargeDir = target.getLocation().toVector()
                        .subtract(entity.getLocation().toVector()).normalize().multiply(2.0);
                chargeDir.setY(0);
                entity.setVelocity(chargeDir);
                model.playAnimation(entity, "run");

                // Damage anything in path
                for (Entity e : entity.getWorld().getNearbyEntities(entity.getLocation(), 1.5, 1.5, 1.5)) {
                    if (e.equals(entity) || e.getUniqueId().equals(owner.getUniqueId())) continue;
                    if (!(e instanceof LivingEntity le)) continue;
                    double chargeDmg = Math.min(20.0, 6.0 + dist * 0.5);
                    le.damage(chargeDmg, owner);
                    Vector kb = le.getLocation().toVector().subtract(entity.getLocation().toVector()).normalize().multiply(2.5);
                    kb.setY(0.6);
                    le.setVelocity(kb);
                }
            } else if (dist <= 5.0) {
                model.playAnimation(entity, "attack");
                target.damage(14.0, owner);
                Vector kb = target.getLocation().toVector().subtract(entity.getLocation().toVector()).normalize().multiply(3.0);
                kb.setY(0.8);
                target.setVelocity(kb);
                entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 1.2f, 0.6f);
                entity.getWorld().spawnParticle(Particle.EXPLOSION, target.getLocation(), 2, 0.3, 0.3, 0.3, 0);
            }
        } else {
            tickGenericFollow(owner, entity);
        }
    }

    // ===== TIGER FUNERAL =====
    // Aggressive melee fighter

    private void tickTigerFuneral(Player owner, LivingEntity entity, ShikigamiInstance instance) {
        LivingEntity target = findNearestHostile(owner, entity, 14.0);

        if (target != null) {
            double dist = entity.getLocation().distance(target.getLocation());
            if (dist <= ATTACK_RANGE) {
                model.playAnimation(entity, "attack");
                target.damage(9.0, owner);
                entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_RAVAGER_ATTACK, 1.0f, 1.2f);
            } else {
                moveToward(entity, target.getLocation(), 1.5);
            }
        } else {
            followOwnerSide(owner, entity, 2.5);
        }
    }

    // ===== MAHORAGA (FRIENDLY) =====

    private void tickMahoragaFriendly(Player owner, LivingEntity entity, ShikigamiInstance instance) {
        LivingEntity target = findNearestHostile(owner, entity, 18.0);

        if (target != null) {
            double dist = entity.getLocation().distance(target.getLocation());
            if (dist <= MAHORAGA_ATTACK_RANGE) {
                tickMahoragaAttackCycle(entity, target, owner, instance, false);
            } else {
                moveToward(entity, target.getLocation(), 1.2);
            }
        } else {
            followOwnerSide(owner, entity, 3.0);
        }
    }

    // ===== MAHORAGA (HOSTILE — RITUAL) =====

    private static final double MAHORAGA_ATTACK_RANGE = 4.5;
    private static final double MAHORAGA_NORMAL_DAMAGE = 14.0;
    private static final double MAHORAGA_UPPERCUT_DAMAGE = 10.0;
    private static final double MAHORAGA_DOWNSLAM_DAMAGE = 22.0;
    private static final double MAHORAGA_UPPERCUT_LAUNCH_Y = 4.0;
    private static final double MAHORAGA_DOWNSLAM_KNOCKBACK = 2.5;

    private void tickMahoragaHostile(Player owner, LivingEntity entity, ShikigamiInstance instance, double dist) {
        if (dist <= MAHORAGA_ATTACK_RANGE) {
            tickMahoragaAttackCycle(entity, owner, owner, instance, true);
        } else if (dist > MAHORAGA_ATTACK_RANGE && dist <= 30.0) {
            moveToward(entity, owner.getLocation(), 1.3);
        }
    }

    /**
     * Mahoraga attack cycle: attack → attack2 → uppercut → (teleport up) → downslam
     */
    private void tickMahoragaAttackCycle(LivingEntity mahoraga, LivingEntity target,
                                         Player damageSource, ShikigamiInstance instance, boolean isHostile) {
        long now = System.currentTimeMillis();
        long lastAttack = instance.summonedAtMs(); // reuse as last attack tracker

        // Simple cooldown via system time stored in the instance
        // We'll use a tag approach
        String cdKey = "mahoraga_atk_" + mahoraga.getUniqueId();
        if (plugin.cooldowns().isOnCooldown(damageSource.getUniqueId(), cdKey)) return;

        // Determine cycle step (0-3)
        int step = mahoraga.getTicksLived() % 4; // rough cycle

        // Use ticks lived modulo to cycle through attacks
        int attackPhase = (mahoraga.getTicksLived() / 30) % 4;

        switch (attackPhase) {
            case 0 -> {
                model.playAnimation(mahoraga, "attack");
                double dmg = MAHORAGA_NORMAL_DAMAGE * (1.0 + instance.adaptationStacks() * 0.05);
                target.damage(dmg, damageSource);
                mahoraga.getWorld().playSound(mahoraga.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 1.0f, 0.8f);
            }
            case 1 -> {
                model.playAnimation(mahoraga, "attack2");
                double dmg = MAHORAGA_NORMAL_DAMAGE * (1.0 + instance.adaptationStacks() * 0.05);
                target.damage(dmg, damageSource);
                mahoraga.getWorld().playSound(mahoraga.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 1.0f, 1.0f);
            }
            case 2 -> {
                // UPPERCUT — launch target 100 blocks into the sky
                model.playAnimation(mahoraga, "uppercut");
                target.damage(MAHORAGA_UPPERCUT_DAMAGE, damageSource);
                target.setVelocity(new Vector(0, MAHORAGA_UPPERCUT_LAUNCH_Y, 0));
                target.setFallDistance(0);
                mahoraga.getWorld().playSound(mahoraga.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 1.2f, 1.4f);
                mahoraga.getWorld().spawnParticle(Particle.EXPLOSION,
                        target.getLocation().add(0, 1, 0), 3, 0.3, 0.3, 0.3, 0);

                // Schedule teleport + downslam
                final LivingEntity finalTarget = target;
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (!mahoraga.isValid() || !finalTarget.isValid()) return;

                    // Teleport Mahoraga to above the target
                    Location above = finalTarget.getLocation().clone().add(0, 3, 0);
                    mahoraga.teleport(above);

                    // Downslam
                    model.playAnimation(mahoraga, "downslam");
                    finalTarget.damage(MAHORAGA_DOWNSLAM_DAMAGE, damageSource);
                    finalTarget.setFallDistance(0);

                    Vector slam = finalTarget.getLocation().toVector()
                            .subtract(mahoraga.getLocation().toVector()).normalize()
                            .multiply(MAHORAGA_DOWNSLAM_KNOCKBACK);
                    slam.setY(-0.5);
                    finalTarget.setVelocity(slam);

                    // Impact effects
                    Location impact = finalTarget.getLocation();
                    mahoraga.getWorld().spawnParticle(Particle.EXPLOSION, impact, 5, 1.0, 0.3, 1.0, 0);
                    mahoraga.getWorld().spawnParticle(Particle.CLOUD, impact, 40, 1.5, 0.3, 1.5, 0.02);
                    mahoraga.getWorld().playSound(impact, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.7f);
                }, 20L);
            }
            case 3 -> {
                // Recovery — Mahoraga adapts
                if (instance.lastPhenomenonType() != null && instance.consecutiveSameHits() >= 3) {
                    instance.addAdaptationStack();

                    // Adaptation visual: the wheel above Mahoraga's head turns
                    model.playAnimation(mahoraga, "adapt");
                    mahoraga.getWorld().spawnParticle(Particle.END_ROD,
                            mahoraga.getLocation().add(0, 3.5, 0), 40, 0.3, 0.3, 0.3, 0.02);
                    mahoraga.getWorld().playSound(mahoraga.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 0.6f);

                    // Announce adaptation
                    for (Player nearby : mahoraga.getWorld().getPlayers()) {
                        if (nearby.getLocation().distance(mahoraga.getLocation()) <= 30) {
                            nearby.sendMessage(plugin.cfg().prefix() +
                                    "§5§lMahoraga is adapting... §7(Reduction: " +
                                    (int)(instance.adaptationReduction() * 100) + "%)");
                        }
                    }
                }
            }
        }

        // Set cooldown to prevent spamming
        plugin.cooldowns().setCooldown(damageSource.getUniqueId(), cdKey, 2);
    }

    // ===== UTILITY =====

    private void tickGenericFollow(Player owner, LivingEntity entity) {
        double dist = entity.getLocation().distance(owner.getLocation());
        if (dist > HARD_LIMIT) {
            Location beside = owner.getLocation().clone().add(1.5, 0, 1.5);
            entity.teleport(beside);
        } else if (dist > FOLLOW_DISTANCE) {
            moveToward(entity, owner.getLocation(), 1.0);
        }
    }

    /**
     * Follow beside the owner, rotating when they rotate.
     */
    private void followOwnerSide(Player owner, LivingEntity entity, double sideOffset) {
        double dist = entity.getLocation().distance(owner.getLocation());
        if (dist > HARD_LIMIT) {
            entity.teleport(getSideLocation(owner, sideOffset));
            return;
        }

        Location sideTarget = getSideLocation(owner, sideOffset);
        double sideDist = entity.getLocation().distance(sideTarget);

        if (sideDist > 1.5) {
            moveToward(entity, sideTarget, 1.0);
        }

        // Face same direction as owner
        Location loc = entity.getLocation().clone();
        loc.setYaw(owner.getLocation().getYaw());
        if (entity instanceof ArmorStand as) {
            as.setRotation(loc.getYaw(), 0);
        }
    }

    private Location getSideLocation(Player owner, double offset) {
        Vector dir = owner.getLocation().getDirection().normalize();
        Vector right = new Vector(-dir.getZ(), 0, dir.getX()).normalize();
        return owner.getLocation().clone().add(right.multiply(offset));
    }

    private void moveToward(LivingEntity entity, Location target, double speed) {
        if (entity instanceof Mob mob) {
            mob.getPathfinder().moveTo(target, speed);
        } else {
            // Fallback: manual velocity
            Vector dir = target.toVector().subtract(entity.getLocation().toVector());
            dir.setY(0);
            if (dir.lengthSquared() > 0.01) {
                dir.normalize().multiply(speed * 0.15);
                dir.setY(entity.getVelocity().getY());
                entity.setVelocity(dir);
            }
        }
    }

    private LivingEntity findNearestHostile(Player owner, LivingEntity shikigami, double range) {
        LivingEntity best = null;
        double bestDist = range;

        for (Entity e : shikigami.getWorld().getNearbyEntities(shikigami.getLocation(), range, range, range)) {
            if (!(e instanceof LivingEntity le)) continue;
            if (e.equals(shikigami)) continue;
            if (e.getUniqueId().equals(owner.getUniqueId())) continue;
            if (e instanceof ArmorStand) continue;
            if (plugin.tenShadows().isShikigamiEntity(e)) continue;
            if (plugin.rika() != null && plugin.rika().isRikaEntity(e)) continue;

            double dist = e.getLocation().distance(shikigami.getLocation());
            if (dist < bestDist) {
                bestDist = dist;
                best = le;
            }
        }
        return best;
    }

    private LivingEntity getOwnerLookTarget(Player owner, double range) {
        Vector look = owner.getEyeLocation().getDirection().normalize();
        Location eye = owner.getEyeLocation();

        LivingEntity best = null;
        double bestAngle = Double.MAX_VALUE;

        for (Entity e : owner.getWorld().getNearbyEntities(eye, range, range, range)) {
            if (!(e instanceof LivingEntity le)) continue;
            if (e.equals(owner)) continue;
            if (e instanceof ArmorStand) continue;
            if (plugin.tenShadows().isShikigamiEntity(e)) continue;

            Vector to = e.getLocation().clone().add(0, 0.8, 0).toVector()
                    .subtract(eye.toVector()).normalize();
            double dot = Math.max(-1.0, Math.min(1.0, look.dot(to)));
            double angle = Math.acos(dot);

            if (angle < Math.toRadians(25) && angle < bestAngle) {
                bestAngle = angle;
                best = le;
            }
        }
        return best;
    }

    private void genericAttack(LivingEntity attacker, LivingEntity target, double damage) {
        target.damage(damage);
        attacker.getWorld().playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 0.8f, 1.0f);
    }

    private void spawnTongueLine(Location from, Location to) {
        World w = from.getWorld();
        if (w == null) return;
        Vector dir = to.toVector().subtract(from.toVector());
        double dist = dir.length();
        dir.normalize();
        Particle.DustOptions green = new Particle.DustOptions(Color.fromRGB(50, 180, 50), 1.2f);
        for (double d = 0; d < dist; d += 0.5) {
            Location p = from.clone().add(dir.clone().multiply(d));
            w.spawnParticle(Particle.DUST, p, 1, 0, 0, 0, 0, green);
        }
    }
}