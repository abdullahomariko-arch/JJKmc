package me.axebanz.jJK;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public final class StrawDollListener implements Listener {

    private final JJKCursedToolsPlugin plugin;
    private final StrawDollManager manager;

    private final NamespacedKey KEY_CURSED_NAIL;
    private final NamespacedKey KEY_NAIL_SHOOTER;
    private final NamespacedKey KEY_DROPPER;

    public StrawDollListener(JJKCursedToolsPlugin plugin, StrawDollManager manager) {
        this.plugin = plugin;
        this.manager = manager;
        this.KEY_CURSED_NAIL = new NamespacedKey(plugin, "straw_doll_nail");
        this.KEY_NAIL_SHOOTER = new NamespacedKey(plugin, "straw_doll_nail_shooter");
        this.KEY_DROPPER = new NamespacedKey(plugin, "straw_doll_dropper");
    }

    // ========== A. Hammer melee — BOW does 0 melee normally, force 7.0 ==========

    @EventHandler(priority = EventPriority.NORMAL)
    public void onMeleeHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player attacker)) return;
        ItemStack mainHand = attacker.getInventory().getItemInMainHand();
        ToolId tool = plugin.tools().identify(mainHand);
        if (tool != ToolId.STRAW_DOLL_HAMMER) return;
        e.setDamage(plugin.cfg().c().getDouble("tools.hammer.attackDamage", 7.0));
    }

    // ========== B. Right-click hammer ==========
    // - Offhand has cursed body → Resonance (cancel bow draw)
    // - Offhand has binding vow tracked item → Resonance (cancel bow draw)
    // - Sneaking + nail target exists → Hairpin (cancel bow draw)
    // - Otherwise → let bow draw normally to shoot nails

    @EventHandler(priority = EventPriority.HIGH)
    public void onRightClickHammer(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (e.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_AIR
                && e.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;

        ItemStack mainHand = p.getInventory().getItemInMainHand();
        ToolId tool = plugin.tools().identify(mainHand);
        if (tool != ToolId.STRAW_DOLL_HAMMER) return;

        String assignedId = plugin.techniqueManager().getAssignedId(p.getUniqueId());
        if (!"strawdoll".equalsIgnoreCase(assignedId)) return;

        ItemStack offHand = p.getInventory().getItemInOffHand();

        // --- Sneaking = Hairpin (check FIRST so sneak always = hairpin) ---
        if (p.isSneaking()) {
            UUID nailTarget = manager.lastNailHitTarget.get(p.getUniqueId());
            if (nailTarget != null) {
                e.setCancelled(true);
                manager.activateHairpin(p);
                return;
            }
            // No nail target but sneaking — still cancel bow draw, tell them
            e.setCancelled(true);
            p.sendMessage(plugin.cfg().prefix() + "§cNo nail target. Shoot someone first.");
            return;
        }

        // --- Not sneaking: Resonance if offhand has cursed body ---
        if (plugin.cursedBody().isCursedBody(offHand)) {
            e.setCancelled(true);
            manager.activateResonance(p);
            return;
        }

        // --- Not sneaking: Resonance via Binding Vow tracked item ---
        if (manager.hasBindingVow(p.getUniqueId())) {
            StrawDollManager.StrawDollBindingVow vow = manager.getBindingVow(p.getUniqueId());
            if (vow != null && vow.trackedItem != null && vow.dropperUuid != null
                    && offHand != null && offHand.isSimilar(vow.trackedItem)) {
                e.setCancelled(true);
                manager.activateResonance(p);
                return;
            }
        }

        // --- Not sneaking, no resonance conditions → let bow draw normally ---
    }

    // ========== C. Tag arrows shot from the hammer as cursed nails ==========

    @EventHandler(priority = EventPriority.NORMAL)
    public void onShootBow(EntityShootBowEvent e) {
        if (!(e.getEntity() instanceof Player shooter)) return;
        ToolId bowTool = plugin.tools().identify(e.getBow());
        if (bowTool != ToolId.STRAW_DOLL_HAMMER) return;
        if (!(e.getProjectile() instanceof Arrow arrow)) return;

        arrow.getPersistentDataContainer().set(KEY_CURSED_NAIL, PersistentDataType.INTEGER, 1);
        arrow.getPersistentDataContainer().set(KEY_NAIL_SHOOTER, PersistentDataType.STRING, shooter.getUniqueId().toString());
    }

    // ========== D. Nail hit — track who got nailed ==========

    @EventHandler(priority = EventPriority.NORMAL)
    public void onProjectileHit(ProjectileHitEvent e) {
        if (!(e.getEntity() instanceof Arrow arrow)) return;
        Integer nail = arrow.getPersistentDataContainer().get(KEY_CURSED_NAIL, PersistentDataType.INTEGER);
        if (nail == null || nail != 1) return;

        String shooterStr = arrow.getPersistentDataContainer().get(KEY_NAIL_SHOOTER, PersistentDataType.STRING);
        if (shooterStr == null) return;
        if (!(e.getHitEntity() instanceof LivingEntity hitEntity)) return;

        try {
            UUID shooterUuid = UUID.fromString(shooterStr);
            manager.lastNailHitTarget.put(shooterUuid, hitEntity.getUniqueId());

            Player shooter = Bukkit.getPlayer(shooterUuid);
            if (shooter != null) {
                String targetName = hitEntity instanceof Player tp ? tp.getName() : hitEntity.getName();
                shooter.sendMessage(plugin.cfg().prefix() + "§6Nail lodged in §f" + targetName + "§6. Sneak + right-click to detonate.");
            }
        } catch (IllegalArgumentException ignored) {}
    }

    // ========== E. Item Pickup Tracking for Binding Vow ==========

    @EventHandler(priority = EventPriority.NORMAL)
    public void onItemPickup(EntityPickupItemEvent e) {
        if (!(e.getEntity() instanceof Player picker)) return;
        UUID pickerUuid = picker.getUniqueId();

        PlayerProfile prof = plugin.data().get(pickerUuid);
        if (!"strawdoll".equals(prof.techniqueId)) return;
        if (!prof.strawDollBindingVowActive) return;

        ItemStack droppedItem = e.getItem().getItemStack();
        if (!droppedItem.hasItemMeta()) return;
        String dropperStr = droppedItem.getItemMeta().getPersistentDataContainer().get(KEY_DROPPER, PersistentDataType.STRING);
        if (dropperStr == null) return;

        try {
            UUID dropperUuid = UUID.fromString(dropperStr);
            if (dropperUuid.equals(pickerUuid)) return;

            manager.setTrackedItem(pickerUuid, droppedItem, dropperUuid);

            Player dropper = Bukkit.getPlayer(dropperUuid);
            String dropperName = dropper != null ? dropper.getName() : Bukkit.getOfflinePlayer(dropperUuid).getName();
            if (dropperName == null) dropperName = "Unknown";

            picker.sendMessage(plugin.cfg().prefix() + "§7This item was dropped by §f" + dropperName + "§7. Resonance available.");
        } catch (IllegalArgumentException ignored) {}
    }

    // ========== F. Item Drop Tracking ==========

    @EventHandler(priority = EventPriority.MONITOR)
    public void onItemDrop(PlayerDropItemEvent e) {
        ItemStack item = e.getItemDrop().getItemStack();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().set(KEY_DROPPER, PersistentDataType.STRING, e.getPlayer().getUniqueId().toString());
        item.setItemMeta(meta);
        e.getItemDrop().setItemStack(item);
    }

    // ========== G. Straw Doll Binding Vow Right-Click ==========

    @EventHandler(priority = EventPriority.HIGH)
    public void onBindingVowInteract(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        if (e.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_AIR
                && e.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;

        ItemStack inHand = player.getInventory().getItemInMainHand();

        PlayerProfile prof = plugin.data().get(player.getUniqueId());
        if (!"strawdoll".equals(prof.techniqueId)) return;

        SeanceManager seanceMgr = plugin.seanceManager();
        if (seanceMgr == null || !seanceMgr.isBindingVow(inHand)) return;

        if (prof.strawDollBindingVowActive) {
            player.sendMessage(plugin.cfg().prefix() + "§7Straw Doll Binding Vow is already active.");
            return;
        }

        e.setCancelled(true);
        manager.activateBindingVow(player);
        inHand.subtract(1);
    }
}