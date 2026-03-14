package me.axebanz.jJK;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BoogieWoogieManager {

    private final JJKCursedToolsPlugin plugin;

    private static final double RANGE = 25.0;
    private static final double MAX_ANGLE = Math.toRadians(38);

    private final Map<UUID, UUID> markedA = new ConcurrentHashMap<>();

    public BoogieWoogieManager(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean hasTechnique(Player p) {
        Technique t = plugin.techniqueManager().getAssigned(p.getUniqueId());
        return t != null && "boogie_woogie".equalsIgnoreCase(t.id());
    }

    /** Check if this player has Boogie Woogie via Copy */
    public boolean hasCopiedTechnique(Player p) {
        if (!plugin.copy().canUseCopy(p)) return false;
        PlayerProfile prof = plugin.data().get(p.getUniqueId());
        return prof.copiedTechniqueId != null
                && prof.copiedTechniqueId.equalsIgnoreCase("boogie_woogie");
    }

    /** Can use either natively or via Copy */
    public boolean canUse(Player p) {
        return hasTechnique(p) || hasCopiedTechnique(p);
    }

    /** Ability 1: swap caster with target */
    public void clapSwap(Player caster) {
        if (!checkCanUse(caster)) return;

        Entity target = findAssistedTargetEntity(caster);
        if (target == null) {
            caster.sendMessage(plugin.cfg().prefix() + "§cNo valid target in front of you.");
            return;
        }
        if (target.getUniqueId().equals(caster.getUniqueId())) return;

        swapInstant(caster, target);
    }

    /**
     * Ability 2 (Option A): /boogiewoogie swap
     * - First use: mark A (must be looking at target)
     * - Second use: swap A with current target B (must be looking at target)
     */
    public void swapMarked(Player caster) {
        if (!checkCanUse(caster)) return;

        Entity look = findAssistedTargetEntity(caster);
        if (look == null) {
            caster.sendMessage(plugin.cfg().prefix() + "§cLook at something to mark/swap.");
            return;
        }

        UUID casterId = caster.getUniqueId();
        UUID aId = markedA.get(casterId);

        if (aId == null) {
            markedA.put(casterId, look.getUniqueId());
            caster.sendMessage(plugin.cfg().prefix() + "§bBoogie Woogie: §fMarked A §7→ §f" + name(look));
            pulseBlue(look.getLocation());
            caster.playSound(caster.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.8f);
            return;
        }

        Entity a = plugin.getServer().getEntity(aId);
        Entity b = look;

        if (a == null || !a.isValid()) {
            markedA.remove(casterId);
            caster.sendMessage(plugin.cfg().prefix() + "§cMarked A no longer exists. Mark again.");
            return;
        }

        if (a.getUniqueId().equals(b.getUniqueId())) {
            caster.sendMessage(plugin.cfg().prefix() + "§cThat's already marked as A. Look at something else for B.");
            return;
        }

        markedA.remove(casterId);
        caster.sendMessage(plugin.cfg().prefix() + "§bBoogie Woogie: §fSWAP §7→ §f" + name(a) + " §7<-> §f" + name(b));
        swapInstant(a, b);
    }

    public void clearMark(Player caster) {
        markedA.remove(caster.getUniqueId());
        caster.sendMessage(plugin.cfg().prefix() + "§7Boogie Woogie mark cleared.");
    }

    private boolean checkCanUse(Player caster) {
        if (!canUse(caster)) {
            caster.sendMessage(plugin.cfg().prefix() + "§cYou don't have Boogie Woogie.");
            return false;
        }
        if (!plugin.techniqueManager().canUseTechniqueActions(caster, true)) return false;
        return true;
    }

    private void swapInstant(Entity a, Entity b) {
        Location la = a.getLocation().clone();
        Location lb = b.getLocation().clone();
        World w = la.getWorld();
        if (w == null) return;

        spawnBlue(w, la);
        spawnBlue(w, lb);

        a.teleportAsync(lb);
        b.teleportAsync(la);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            spawnBlue(w, a.getLocation());
            spawnBlue(w, b.getLocation());
            w.playSound(a.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.6f);
            w.playSound(b.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.6f);
        }, 1L);
    }

    private void pulseBlue(Location loc) {
        World w = loc.getWorld();
        if (w == null) return;
        spawnBlue(w, loc);
    }

    private void spawnBlue(World w, Location loc) {
        Location mid = loc.clone().add(0, 1.0, 0);
        Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(0, 120, 255), 1.6f);
        w.spawnParticle(Particle.DUST, mid, 55, 0.45, 0.6, 0.45, 0, dust);
    }

    private Entity findAssistedTargetEntity(Player caster) {
        Vector look = caster.getEyeLocation().getDirection().normalize();
        Location eye = caster.getEyeLocation();

        Entity best = null;
        double bestAngle = Double.MAX_VALUE;
        double bestDist = Double.MAX_VALUE;

        for (Entity e : caster.getWorld().getEntities()) {
            if (!e.isValid()) continue;
            if (e.equals(caster)) continue;

            if (!(e instanceof Player) && !(e instanceof LivingEntity) && !(e instanceof Item)) continue;

            double dist = e.getLocation().distance(eye);
            if (dist > RANGE) continue;

            Location aimAt = e.getLocation().clone().add(0, 0.8, 0);
            Vector to = aimAt.toVector().subtract(eye.toVector()).normalize();

            double dot = clampMinus1To1(look.dot(to));
            double angle = Math.acos(dot);
            if (angle > MAX_ANGLE) continue;

            if (e instanceof Player && !caster.hasLineOfSight(e)) continue;

            if (angle < bestAngle - 1e-6 || (Math.abs(angle - bestAngle) < 1e-6 && dist < bestDist)) {
                best = e;
                bestAngle = angle;
                bestDist = dist;
            }
        }

        return best;
    }

    private double clampMinus1To1(double v) {
        return Math.max(-1.0, Math.min(1.0, v));
    }

    private String name(Entity e) {
        if (e instanceof Player p) return p.getName();
        if (e instanceof Item it) return "Item(" + it.getItemStack().getType().name() + ")";
        return e.getType().name();
    }
}