package me.axebanz.jJK;

import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Domain Expansion: Deadly Sentencing
 * No violence inside. Judgeman presides over a trial.
 */
public final class DeadlySentencingDomain extends DomainExpansion implements Listener {

    private static final int RADIUS = 30;
    private ArmorStand judgemanStand = null;
    private final List<BukkitTask> pendingTasks = new ArrayList<>();
    private final List<Player> defendants = new ArrayList<>();

    public DeadlySentencingDomain(JJKCursedToolsPlugin plugin, Player caster) {
        super(plugin, caster);
        this.refinement = 7.0;
    }

    @Override public String getName() { return "Deadly Sentencing"; }
    @Override public int getRadius() { return RADIUS; }

    @Override
    public void buildInterior() {
        World w = center.getWorld();
        if (w == null) return;

        // Gold/yellow ambiance
        Particle.DustOptions gold = new Particle.DustOptions(Color.fromRGB(200, 150, 0), 1.5f);
        for (int i = 0; i < 150; i++) {
            double r = RADIUS * Math.random();
            double theta = Math.random() * Math.PI * 2;
            double phi = Math.random() * Math.PI;
            double x = r * Math.sin(phi) * Math.cos(theta);
            double y = r * Math.cos(phi);
            double z = r * Math.sin(phi) * Math.sin(theta);
            w.spawnParticle(Particle.DUST, center.clone().add(x, y, z), 1, 0, 0, 0, 0, gold);
        }

        w.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1.5f, 0.7f);
        w.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_AMBIENT, 1.0f, 1.0f);

        // Spawn Judgeman (invisible armor stand behind caster)
        Location judgePos = caster.getLocation().clone().add(
                caster.getLocation().getDirection().multiply(-3));
        judgePos.setYaw(caster.getLocation().getYaw() + 180);

        judgemanStand = w.spawn(judgePos, ArmorStand.class, stand -> {
            stand.setInvisible(true);
            stand.setCustomName("§6§lJudgeman");
            stand.setCustomNameVisible(true);
            stand.setGravity(false);
            stand.setInvulnerable(true);
        });

        // Collect defendants (players inside who are not the caster)
        defendants.clear();
        for (Player p : getPlayersInside()) {
            if (!p.equals(caster)) defendants.add(p);
        }

        // Register damage-cancel listener
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        // Start trial announcements
        startTrial();
    }

    private void startTrial() {
        // Broadcast trial start
        for (Player p : getPlayersInside()) {
            p.sendMessage("§6§l[JUDGEMAN] §eOrder in the court! The domain of Deadly Sentencing has been opened.");
        }

        if (!defendants.isEmpty()) {
            String defendantNames = String.join(", ", defendants.stream().map(Player::getName).toList());
            pendingTasks.add(Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!active) return;
                for (Player p : getPlayersInside()) {
                    p.sendMessage("§6§l[JUDGEMAN] §eThe defendant(s) — §f" + defendantNames + " §e— stand accused.");
                    p.sendMessage("§6§l[JUDGEMAN] §eIn this domain, no violence shall be permitted.");
                    p.sendMessage("§6§l[JUDGEMAN] §eThe court will now determine your sentence.");
                }
            }, 40L));

            pendingTasks.add(Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!active) return;
                for (Player p : getPlayersInside()) {
                    p.sendMessage("§6§l[JUDGEMAN] §eAfter careful deliberation...");
                }
            }, 80L));

            pendingTasks.add(Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!active) return;
                for (Player p : getPlayersInside()) {
                    p.sendMessage("§6§l[JUDGEMAN] §c§lGUILTY! §eThe sentence shall be carried out!");
                }
                // Collapse domain when verdict is delivered
                if (plugin.domainManager() != null) {
                    pendingTasks.add(Bukkit.getScheduler().runTaskLater(plugin, () ->
                            plugin.domainManager().collapse(caster), 40L));
                }
            }, 140L));
        } else {
            pendingTasks.add(Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!active) return;
                for (Player p : getPlayersInside()) {
                    p.sendMessage("§6§l[JUDGEMAN] §eNo defendants present. Court is adjourned.");
                }
                if (plugin.domainManager() != null) {
                    pendingTasks.add(Bukkit.getScheduler().runTaskLater(plugin, () ->
                            plugin.domainManager().collapse(caster), 20L));
                }
            }, 40L));
        }
    }

    /** Block all damage inside the domain */
    @EventHandler
    public void onDamageInside(EntityDamageByEntityEvent e) {
        if (!active) return;
        if (isInside(e.getEntity().getLocation())) {
            e.setCancelled(true);
        }
    }

    @Override
    public void onSureHit(Player target) {
        // No sure-hit — domain is a no-violence zone
    }

    @Override
    public void onTick() {
        World w = center.getWorld();
        if (w == null) return;

        // Ambient gold particles
        Particle.DustOptions gold = new Particle.DustOptions(Color.fromRGB(200, 150, 0), 1.2f);
        for (int i = 0; i < 20; i++) {
            double r = RADIUS * Math.random();
            double theta = Math.random() * Math.PI * 2;
            double phi = Math.random() * Math.PI;
            double x = r * Math.sin(phi) * Math.cos(theta);
            double y = r * Math.cos(phi);
            double z = r * Math.sin(phi) * Math.sin(theta);
            w.spawnParticle(Particle.DUST, center.clone().add(x, y, z), 1, 0, 0, 0, 0, gold);
        }

        // Keep Judgeman looking at defendants
        if (judgemanStand != null && judgemanStand.isValid() && !defendants.isEmpty()) {
            Player firstDef = defendants.get(0);
            if (firstDef.isOnline()) {
                Location judgemanLoc = judgemanStand.getLocation();
                Location defLoc = firstDef.getLocation().add(0, 1, 0);
                double dx = defLoc.getX() - judgemanLoc.getX();
                double dz = defLoc.getZ() - judgemanLoc.getZ();
                float yaw = (float) (Math.toDegrees(Math.atan2(-dx, dz)));
                judgemanLoc.setYaw(yaw);
                judgemanStand.teleport(judgemanLoc);
            }
        }
    }

    @Override
    public void onDomainEnd() {
        HandlerList.unregisterAll(this);

        // Cancel pending trial tasks
        for (BukkitTask task : pendingTasks) {
            task.cancel();
        }
        pendingTasks.clear();

        if (judgemanStand != null && judgemanStand.isValid()) {
            judgemanStand.remove();
        }

        for (Player p : getPlayersInside()) {
            p.sendMessage("§6§l[JUDGEMAN] §eThe court has been adjourned.");
        }
    }
}
