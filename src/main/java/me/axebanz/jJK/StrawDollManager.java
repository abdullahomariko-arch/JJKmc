package me.axebanz.jJK;

import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class StrawDollManager {

    private final JJKCursedToolsPlugin plugin;

    /** Tracks the last entity hit by a nail per caster UUID */
    final Map<UUID, UUID> lastNailHitTarget = new ConcurrentHashMap<>();

    /** Tracks binding vow data per player UUID */
    private final Map<UUID, StrawDollBindingVow> activeBindingVows = new ConcurrentHashMap<>();

    public StrawDollManager(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    // ========== Binding Vow ==========

    public static final class StrawDollBindingVow {
        /** UUID of the player who owns the binding vow */
        public final UUID ownerUuid;
        /** The item picked up from an enemy (unmodified, for comparison) */
        public ItemStack trackedItem;
        /** UUID of the enemy who dropped the tracked item */
        public UUID dropperUuid;

        public StrawDollBindingVow(UUID ownerUuid) {
            this.ownerUuid = ownerUuid;
        }
    }

    public void activateBindingVow(Player player) {
        PlayerProfile prof = plugin.data().get(player.getUniqueId());
        prof.strawDollBindingVowActive = true;
        plugin.data().save(player.getUniqueId());
        activeBindingVows.put(player.getUniqueId(), new StrawDollBindingVow(player.getUniqueId()));
        player.sendMessage(plugin.cfg().prefix() + "§5Binding Vow activated. §7Resonance can now target any enemy's dropped item. Hairpin damage reduced.");
    }

    public boolean hasBindingVow(UUID uuid) {
        PlayerProfile prof = plugin.data().get(uuid);
        return prof.strawDollBindingVowActive;
    }

    public StrawDollBindingVow getBindingVow(UUID uuid) {
        return activeBindingVows.get(uuid);
    }

    /** Store the tracked item (original, unmodified) and the dropper UUID for binding vow resonance. */
    public void setTrackedItem(UUID playerUuid, ItemStack item, UUID dropperUuid) {
        StrawDollBindingVow vow = activeBindingVows.get(playerUuid);
        if (vow != null) {
            vow.trackedItem = item.clone();
            vow.dropperUuid = dropperUuid;
        }
    }

    // ========== Resonance (Ability 1) ==========

    public void activateResonance(Player player) {
        String prefix = plugin.cfg().prefix();
        UUID playerUuid = player.getUniqueId();

        // Check CE
        if (!plugin.ce().tryConsume(playerUuid, 3)) {
            player.sendMessage(prefix + "§cNot enough Cursed Energy. (Need 3)");
            return;
        }

        // Check cooldown
        if (plugin.cooldowns().isOnCooldown(playerUuid, "strawdoll_resonance")) {
            player.sendMessage(prefix + "§cResonance is on cooldown.");
            plugin.ce().add(playerUuid, 3);
            return;
        }

        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();

        // Check main hand is straw_doll_hammer (for command usage; listener already checks)
        ToolId mainHandTool = plugin.tools().identify(mainHand);
        if (mainHandTool != ToolId.STRAW_DOLL_HAMMER) {
            player.sendMessage(prefix + "§cYou need the Straw Doll Hammer in your main hand.");
            plugin.ce().add(playerUuid, 3);
            return;
        }

        UUID targetUuid = null;
        boolean usingBindingVow = false;

        if (hasBindingVow(playerUuid)) {
            // Binding vow resonance: use tracked item in offhand
            StrawDollBindingVow vow = getBindingVow(playerUuid);
            if (vow != null && vow.trackedItem != null && vow.dropperUuid != null
                    && offHand != null && offHand.isSimilar(vow.trackedItem)) {
                targetUuid = vow.dropperUuid;
                usingBindingVow = true;
            }
        }

        if (!usingBindingVow) {
            // Normal resonance: offhand must be a cursed body
            if (!plugin.cursedBody().isCursedBody(offHand)) {
                player.sendMessage(prefix + "§cYou need a Cursed Body in your offhand (or a tracked enemy item with Binding Vow).");
                plugin.ce().add(playerUuid, 3);
                return;
            }
            targetUuid = plugin.cursedBody().source(offHand);
            if (targetUuid == null) {
                player.sendMessage(prefix + "§cThis Cursed Body has no valid source.");
                plugin.ce().add(playerUuid, 3);
                return;
            }
        }

        // Consume item from offhand
        if (offHand != null && offHand.getAmount() > 0) {
            offHand.subtract(1);
            player.getInventory().setItemInOffHand(offHand.getAmount() <= 0 ? null : offHand);
        }

        // If using binding vow, clear tracked item
        if (usingBindingVow) {
            StrawDollBindingVow vow = getBindingVow(playerUuid);
            if (vow != null) {
                vow.trackedItem = null;
                vow.dropperUuid = null;
            }
        }

        plugin.cooldowns().setCooldown(playerUuid, "strawdoll_resonance", 30);

        final UUID finalTargetUuid = targetUuid;
        // 2.5 second delay (50 ticks), then deal damage
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player target = Bukkit.getPlayer(finalTargetUuid);
            if (target == null || !target.isOnline()) {
                player.sendMessage(prefix + "§7Resonance fizzled — target is no longer present.");
                return;
            }

            // Spawn particles at caster and target
            player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, player.getLocation().add(0, 1, 0), 30, 0.3, 0.5, 0.3, 0.05);
            target.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, target.getLocation().add(0, 1, 0), 30, 0.3, 0.5, 0.3, 0.05);

            // Sound at target
            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 1.0f, 1.0f);

            // Deal 4 hearts (8 HP)
            target.damage(8.0, player);

            player.sendMessage(prefix + "§6Resonance struck §f" + target.getName() + "§6!");
        }, 50L);

        player.sendMessage(prefix + "§6Resonance activated — strike incoming in 2.5 seconds.");
    }

    // ========== Hairpin (Ability 2) ==========

    public void activateHairpin(Player player) {
        String prefix = plugin.cfg().prefix();
        UUID playerUuid = player.getUniqueId();

        UUID targetUuid = lastNailHitTarget.get(playerUuid);
        if (targetUuid == null) {
            player.sendMessage(prefix + "§cNo nail target. Shoot an arrow from your Straw Doll Hammer first.");
            return;
        }

        LivingEntity target = null;
        for (org.bukkit.World w : Bukkit.getWorlds()) {
            for (org.bukkit.entity.Entity e : w.getEntities()) {
                if (e.getUniqueId().equals(targetUuid) && e instanceof LivingEntity le) {
                    target = le;
                    break;
                }
            }
            if (target != null) break;
        }

        if (target == null) {
            player.sendMessage(prefix + "§cNail target is no longer present.");
            lastNailHitTarget.remove(playerUuid);
            return;
        }

        // Check CE
        if (!plugin.ce().tryConsume(playerUuid, 2)) {
            player.sendMessage(prefix + "§cNot enough Cursed Energy. (Need 2)");
            return;
        }

        // Check cooldown
        if (plugin.cooldowns().isOnCooldown(playerUuid, "strawdoll_hairpin")) {
            player.sendMessage(prefix + "§cHairpin is on cooldown.");
            plugin.ce().add(playerUuid, 2);
            return;
        }

        plugin.cooldowns().setCooldown(playerUuid, "strawdoll_hairpin", 5);

        // Determine damage based on binding vow (1.5 hearts = 3HP vs 2 hearts = 4HP)
        double damage = hasBindingVow(playerUuid) ? 3.0 : 4.0;

        // Explosion effect at target
        target.getWorld().spawnParticle(Particle.EXPLOSION, target.getLocation().add(0, 1, 0), 5, 0.3, 0.3, 0.3, 0.1);
        target.getWorld().spawnParticle(Particle.FLAME, target.getLocation().add(0, 1, 0), 20, 0.3, 0.3, 0.3, 0.05);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);

        target.damage(damage, player);

        lastNailHitTarget.remove(playerUuid);

        String targetName = target instanceof Player tp ? tp.getName() : target.getName();
        player.sendMessage(prefix + "§6Hairpin detonated on §f" + targetName + "§6!");
    }
}