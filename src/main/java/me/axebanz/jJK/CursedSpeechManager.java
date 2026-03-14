package me.axebanz.jJK;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CursedSpeechManager {

    private final JJKCursedToolsPlugin plugin;

    // ===== "Study rule" (water -> 3 casts total) =====
    private static final int WATER_CHARGES = 3;
    private final Map<UUID, Integer> charges = new ConcurrentHashMap<>();

    // NoMove state
    private final Map<UUID, Long> noMoveUntilMs = new ConcurrentHashMap<>();

    // Cooldowns
    private static final long COOLDOWN_SECONDS = 30;
    private static final long NOMOVE_SECONDS = 5;

    // CE costs
    private static final int CE_NOMOVE = 3;
    private static final int CE_PLUMMET = 7;
    private static final int CE_EXPLODE = 5;

    // Crater spec
    private static final int CRATER_RADIUS = 12;
    private static final int CRATER_DEPTH = 9;
    private static final double FLOOR_RADIUS_FACTOR = 0.45;

    // Aim assist
    private static final double TARGET_RANGE = 22.0;
    private static final double MAX_VIEW_ANGLE_RADIANS = Math.toRadians(28);

    public CursedSpeechManager(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean hasTechnique(Player p) {
        Technique t = plugin.techniqueManager().getAssigned(p.getUniqueId());
        return t != null && "cursed_speech".equalsIgnoreCase(t.id());
    }

    /** Check if this player has Cursed Speech via Copy */
    public boolean hasCopiedTechnique(Player p) {
        if (!plugin.copy().canUseCopy(p)) return false;
        PlayerProfile prof = plugin.data().get(p.getUniqueId());
        return prof.copiedTechniqueId != null
                && prof.copiedTechniqueId.equalsIgnoreCase("cursed_speech");
    }

    /** Can use either natively or via Copy */
    public boolean canUse(Player p) {
        return hasTechnique(p) || hasCopiedTechnique(p);
    }

    public void onDrinkWater(Player p) {
        charges.put(p.getUniqueId(), WATER_CHARGES);
        p.sendMessage(plugin.cfg().prefix() + "§aThroat refreshed. §7Charges: §f" + WATER_CHARGES + "§7/§f" + WATER_CHARGES);
        p.playSound(p.getLocation(), Sound.ENTITY_GENERIC_DRINK, 0.8f, 1.2f);
    }

    public int remainingCharges(UUID uuid) {
        return Math.max(0, charges.getOrDefault(uuid, 0));
    }

    public boolean isNoMove(UUID uuid) {
        return noMoveUntilMs.getOrDefault(uuid, 0L) > System.currentTimeMillis();
    }

    private boolean checkCanCast(Player caster, String key, String prettyName) {
        if (!canUse(caster)) {
            caster.sendMessage(plugin.cfg().prefix() + "§cYou don't have Cursed Speech.");
            return false;
        }

        if (!plugin.techniqueManager().canUseTechniqueActions(caster, true)) return false;

        int ch = remainingCharges(caster.getUniqueId());
        if (ch <= 0) {
            caster.sendMessage(plugin.cfg().prefix() + "§bDrink a Bottle Of water to use Cursed speach.");
            caster.damage(2.0);
            return false;
        }

        if (plugin.cooldowns().isOnCooldown(caster.getUniqueId(), key)) {
            long rem = plugin.cooldowns().remainingSeconds(caster.getUniqueId(), key);
            plugin.actionbarUI().setTimer(caster.getUniqueId(), key, "■", "§a", rem);
            caster.sendMessage(plugin.cfg().prefix() + "§c" + prettyName + " is on cooldown: §f" + TimeFmt.mmss(rem));
            return false;
        }

        return true;
    }

    private boolean tryConsumeCe(Player caster, int cost, String prettyName) {
        if (plugin.ce().tryConsume(caster.getUniqueId(), cost)) return true;

        caster.sendMessage(plugin.cfg().prefix() + "§cNot enough Cursed Energy for §f" + prettyName + "§c. §7(Need " + cost + ")");
        caster.playSound(caster.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.6f);
        return false;
    }

    private void consumeCharge(Player caster) {
        UUID u = caster.getUniqueId();
        int ch = remainingCharges(u);
        if (ch <= 0) return;
        charges.put(u, ch - 1);
        caster.sendMessage(plugin.cfg().prefix() + "§7Cursed Speech charges: §f" + (ch - 1) + "§7/§f" + WATER_CHARGES);
    }

    public LivingEntity findAssistedTarget(Player caster) {
        Vector look = caster.getEyeLocation().getDirection().normalize();
        Location eye = caster.getEyeLocation();

        LivingEntity best = null;
        double bestAngle = Double.MAX_VALUE;
        double bestDist = Double.MAX_VALUE;

        for (LivingEntity le : caster.getWorld().getLivingEntities()) {
            if (le.equals(caster)) continue;
            if (!le.isValid()) continue;

            double dist = le.getLocation().distance(eye);
            if (dist > TARGET_RANGE) continue;

            Location targetLoc = le.getLocation().clone().add(0, Math.max(0.8, le.getHeight() * 0.6), 0);
            Vector to = targetLoc.toVector().subtract(eye.toVector()).normalize();

            double dot = clampMinus1To1(look.dot(to));
            double angle = Math.acos(dot);

            if (angle > MAX_VIEW_ANGLE_RADIANS) continue;

            if (angle < bestAngle - 1e-6 || (Math.abs(angle - bestAngle) < 1e-6 && dist < bestDist)) {
                if (le instanceof Player && !caster.hasLineOfSight(le)) continue;

                best = le;
                bestAngle = angle;
                bestDist = dist;
            }
        }

        return best;
    }

    // ===== Abilities =====

    public void castNoMove(Player caster) {
        String key = "cursed_speech.nomove";
        if (!checkCanCast(caster, key, "Don't Move")) return;

        LivingEntity target = findAssistedTarget(caster);
        if (target == null) {
            caster.sendMessage(plugin.cfg().prefix() + "§cNo target in front of you.");
            return;
        }

        if (!tryConsumeCe(caster, CE_NOMOVE, "Don't Move")) return;
        consumeCharge(caster);

        long until = System.currentTimeMillis() + NOMOVE_SECONDS * 1000L;
        noMoveUntilMs.put(target.getUniqueId(), until);

        target.setVelocity(new Vector(0, 0, 0));
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, (int) (NOMOVE_SECONDS * 20), 10, false, false, true));
        target.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, (int) (NOMOVE_SECONDS * 20), 250, false, false, true));

        Location mid = target.getLocation().clone().add(0, 1.0, 0);
        target.getWorld().spawnParticle(Particle.ENCHANT, mid, 70, 0.45, 0.65, 0.45, 0.01);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.6f, 1.6f);

        caster.sendMessage(plugin.cfg().prefix() + "§aCursed Speech: §fDON'T MOVE §7→ " + name(target));
        if (target instanceof Player tp) tp.sendMessage(plugin.cfg().prefix() + "§cCursed Speech: §fDON'T MOVE!");

        plugin.cooldowns().setCooldown(caster.getUniqueId(), key, COOLDOWN_SECONDS);
        plugin.actionbarUI().setTimer(caster.getUniqueId(), key, "■", "§a", COOLDOWN_SECONDS);
    }

    public void castPlummet(Player caster) {
        String key = "cursed_speech.plummet";
        if (!checkCanCast(caster, key, "Plummet")) return;

        LivingEntity target = findAssistedTarget(caster);
        if (target == null) {
            caster.sendMessage(plugin.cfg().prefix() + "§cNo target in front of you.");
            return;
        }

        if (!tryConsumeCe(caster, CE_PLUMMET, "Plummet")) return;
        consumeCharge(caster);

        final int slamTicks = 20;
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 0.6f, 1.6f);

        for (int i = 0; i < slamTicks; i++) {
            int t = i;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!target.isValid()) return;

                Vector vel = target.getVelocity();
                target.setVelocity(new Vector(vel.getX() * 0.06, -7.0, vel.getZ() * 0.06));
                target.setFallDistance(0f);
            }, t);
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!target.isValid()) return;

            Location impact = target.getLocation().clone();
            World w = impact.getWorld();
            if (w == null) return;

            makeCleanBowlCrater(impact, CRATER_RADIUS, CRATER_DEPTH);

            w.playSound(impact, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.75f);
            w.spawnParticle(Particle.EXPLOSION, impact.clone().add(0, 0.5, 0), 1, 0, 0, 0, 0);
            w.spawnParticle(Particle.CLOUD, impact.clone().add(0, 0.2, 0), 140, 2.0, 0.25, 2.0, 0.02);

            spawnRing(impact.clone().add(0, 0.12, 0), 8.0, 140, Color.RED);
            spawnRing(impact.clone().add(0, 0.12, 0), 8.0, 140, Color.BLUE);

            double dmg = Math.min(14.0, target.getHealth() - 1.0);
            if (dmg > 0) target.damage(dmg, caster);

            target.setVelocity(new Vector(0, 0.25, 0));
        }, slamTicks + 2L);

        caster.sendMessage(plugin.cfg().prefix() + "§aCursed Speech: §fPLUMMET §7→ " + name(target));

        plugin.cooldowns().setCooldown(caster.getUniqueId(), key, COOLDOWN_SECONDS);
        plugin.actionbarUI().setTimer(caster.getUniqueId(), key, "■", "§a", COOLDOWN_SECONDS);
    }

    public void castExplode(Player caster) {
        String key = "cursed_speech.explode";
        if (!checkCanCast(caster, key, "Explode")) return;

        LivingEntity target = findAssistedTarget(caster);
        if (target == null) {
            caster.sendMessage(plugin.cfg().prefix() + "§cNo target in front of you.");
            return;
        }

        if (!tryConsumeCe(caster, CE_EXPLODE, "Explode")) return;
        consumeCharge(caster);

        Location loc = target.getLocation().clone();
        World w = loc.getWorld();
        if (w == null) return;

        w.spawnParticle(Particle.EXPLOSION, loc.clone().add(0, 0.6, 0), 1, 0, 0, 0, 0);
        w.spawnParticle(Particle.FLAME, loc.clone().add(0, 0.7, 0), 50, 0.7, 0.45, 0.7, 0.02);
        w.spawnParticle(Particle.SMOKE, loc.clone().add(0, 0.7, 0), 35, 0.7, 0.45, 0.7, 0.02);
        w.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.2f);

        double dmg = Math.min(16.0, target.getHealth() - 1.0);
        if (dmg > 0) target.damage(dmg, caster);

        Vector kb = target.getLocation().toVector().subtract(caster.getLocation().toVector()).normalize().multiply(1.6);
        kb.setY(0.55);
        target.setVelocity(kb);

        caster.sendMessage(plugin.cfg().prefix() + "§aCursed Speech: §fEXPLODE §7→ " + name(target));

        plugin.cooldowns().setCooldown(caster.getUniqueId(), key, COOLDOWN_SECONDS);
        plugin.actionbarUI().setTimer(caster.getUniqueId(), key, "■", "§a", COOLDOWN_SECONDS);
    }

    // ===== Clean crater implementation =====

    private void makeCleanBowlCrater(Location impact, int R, int D) {
        World w = impact.getWorld();
        if (w == null) return;

        int cx = impact.getBlockX();
        int cz = impact.getBlockZ();

        int surfaceCenter = w.getHighestBlockYAt(cx, cz) - 1;
        int size = R * 2 + 1;

        int[][] ty = new int[size][size];
        boolean[][] in = new boolean[size][size];

        for (int dx = -R; dx <= R; dx++) {
            for (int dz = -R; dz <= R; dz++) {
                double r = Math.sqrt(dx * dx + dz * dz);
                if (r > R) continue;

                double t = clamp01(r / (double) R);
                double depth = D * (1.0 - (t * t));

                int x = cx + dx;
                int z = cz + dz;

                int surfaceY = findSurfaceNear(w, x, z, surfaceCenter, 12);
                int targetY = surfaceY - (int) Math.round(depth);

                int ix = dx + R;
                int iz = dz + R;
                ty[ix][iz] = targetY;
                in[ix][iz] = true;
            }
        }

        smoothOnce(ty, in, R);
        smoothOnce(ty, in, R);

        double Rf = FLOOR_RADIUS_FACTOR * R;

        for (int dx = -R; dx <= R; dx++) {
            for (int dz = -R; dz <= R; dz++) {
                int ix = dx + R;
                int iz = dz + R;
                if (!in[ix][iz]) continue;

                double r = Math.sqrt(dx * dx + dz * dz);
                double t = clamp01(r / (double) R);

                int x = cx + dx;
                int z = cz + dz;

                int surfaceY = findSurfaceNear(w, x, z, surfaceCenter, 12);
                int targetY = ty[ix][iz];

                for (int y = surfaceY; y > targetY; y--) {
                    Block b = w.getBlockAt(x, y, z);
                    if (b.getType() == Material.BEDROCK) break;
                    if (b.isLiquid()) continue;
                    b.setType(Material.AIR, false);
                }

                Block floor = w.getBlockAt(x, targetY, z);
                if (floor.getType() != Material.BEDROCK && !floor.isLiquid()) {
                    if (t > 0.75) {
                        floor.setType(Material.DIRT, false);
                        if (rngChance(0.25)) floor.setType(Material.GRASS_BLOCK, false);
                    } else {
                        floor.setType(Material.STONE, false);
                    }
                }

                if (r >= (R - 1) && r <= R && rngChance(0.60)) {
                    int topY = findSurfaceNear(w, x, z, surfaceCenter, 14);
                    Block top = w.getBlockAt(x, topY + 1, z);
                    if (top.getY() < w.getMaxHeight() && top.getType().isAir()) {
                        top.setType(Material.DIRT, false);
                        if (rngChance(0.50)) top.setType(Material.GRASS_BLOCK, false);
                    }
                }

                if (r <= Rf && rngChance(0.02)) {
                    Block patch = w.getBlockAt(x, targetY, z);
                    if (patch.getType() == Material.STONE) patch.setType(Material.DIRT, false);
                }
            }
        }
    }

    private void smoothOnce(int[][] targetY, boolean[][] inside, int R) {
        int size = R * 2 + 1;
        int[][] copy = new int[size][size];
        for (int x = 0; x < size; x++) System.arraycopy(targetY[x], 0, copy[x], 0, size);

        for (int x = 1; x < size - 1; x++) {
            for (int z = 1; z < size - 1; z++) {
                if (!inside[x][z]) continue;

                int sum = 0;
                int cnt = 0;

                if (inside[x - 1][z]) { sum += copy[x - 1][z]; cnt++; }
                if (inside[x + 1][z]) { sum += copy[x + 1][z]; cnt++; }
                if (inside[x][z - 1]) { sum += copy[x][z - 1]; cnt++; }
                if (inside[x][z + 1]) { sum += copy[x][z + 1]; cnt++; }

                if (cnt == 0) continue;
                int avg = Math.round(sum / (float) cnt);

                if (copy[x][z] > avg + 2) targetY[x][z] = copy[x][z] - 1;
                else if (copy[x][z] < avg - 2) targetY[x][z] = copy[x][z] + 1;
            }
        }
    }

    private int findSurfaceNear(World w, int x, int z, int aroundY, int scan) {
        for (int y = aroundY + scan; y >= aroundY - scan; y--) {
            if (y <= w.getMinHeight()) break;
            Block b = w.getBlockAt(x, y, z);
            if (!b.getType().isAir() && !b.isLiquid()) return y;
        }
        return w.getHighestBlockYAt(x, z) - 1;
    }

    private boolean rngChance(double p) {
        return Math.random() < p;
    }

    private double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }

    private double clampMinus1To1(double v) {
        return Math.max(-1.0, Math.min(1.0, v));
    }

    private String name(LivingEntity e) {
        if (e instanceof Player p) return p.getName();
        return e.getType().name();
    }

    private void spawnRing(Location center, double radius, int points, Color color) {
        World world = center.getWorld();
        if (world == null) return;

        Particle.DustOptions opts = new Particle.DustOptions(color, 1.2f);
        for (int i = 0; i < points; i++) {
            double a = (Math.PI * 2.0) * (i / (double) points);
            double x = Math.cos(a) * radius;
            double z = Math.sin(a) * radius;
            Location p = center.clone().add(x, 0.0, z);
            world.spawnParticle(Particle.DUST, p, 1, 0, 0, 0, 0, opts);
        }
    }
}