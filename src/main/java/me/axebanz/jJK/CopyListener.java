package me.axebanz.jJK;

import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CopyListener implements Listener {

    private final JJKCursedToolsPlugin plugin;
    private final RikaManager rika;
    private final RikaStorageGUI storage;
    private final CursedBodyItem cursedBody;

    private static final double DROP_CHANCE_NORMAL = 0.05;
    private static final double DROP_CHANCE_COPY = 0.07;
    private static final long DROP_COOLDOWN_MS = 3000;

    private final Random rng = new Random();
    private final Map<String, Long> lastDropTime = new ConcurrentHashMap<>();

    public CopyListener(JJKCursedToolsPlugin plugin, RikaManager rika, RikaStorageGUI storage, CursedBodyItem cursedBody) {
        this.plugin = plugin;
        this.rika = rika;
        this.storage = storage;
        this.cursedBody = cursedBody;
    }

    @EventHandler
    public void onHitPlayer(EntityDamageByEntityEvent e) {
        if (e.isCancelled()) return;
        if (!(e.getDamager() instanceof Player attacker)) return;
        if (!(e.getEntity() instanceof Player victim)) return;

        if (plugin.techniqueManager().getAssigned(victim.getUniqueId()) == null) return;

        String pairKey = attacker.getUniqueId() + ":" + victim.getUniqueId();
        long now = System.currentTimeMillis();
        Long lastDrop = lastDropTime.get(pairKey);
        if (lastDrop != null && (now - lastDrop) < DROP_COOLDOWN_MS) return;

        String attackerTech = plugin.techniqueManager().getAssignedId(attacker.getUniqueId());
        double dropChance = "copy".equalsIgnoreCase(attackerTech) ? DROP_CHANCE_COPY : DROP_CHANCE_NORMAL;

        if (rng.nextDouble() > dropChance) return;

        lastDropTime.put(pairKey, now);

        ItemStack body = cursedBody.create(victim.getUniqueId());
        victim.getWorld().dropItemNaturally(victim.getLocation(), body);
    }

    @EventHandler
    public void onInteractRika(PlayerInteractAtEntityEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        Player p = e.getPlayer();
        Entity clicked = e.getRightClicked();
        if (!rika.isRikaEntity(clicked)) return;
        UUID owner = rika.ownerOf(clicked);
        if (owner == null || !owner.equals(p.getUniqueId())) return;
        if (!plugin.copy().canUseCopy(p)) {
            p.sendMessage(plugin.cfg().prefix() + "§cYou must have §dCopy§c equipped to interact with Rika.");
            return;
        }
        if (p.isSneaking()) { e.setCancelled(true); storage.open(p); return; }

        ItemStack hand = p.getInventory().getItemInMainHand();
        if (!cursedBody.isCursedBody(hand)) return;
        UUID source = cursedBody.source(hand);
        if (source == null) { p.sendMessage(plugin.cfg().prefix() + "§cThis Cursed Body is corrupted."); return; }

        plugin.data().load(source);
        String techId = plugin.techniqueManager().getAssignedId(source);
        if (techId == null) { p.sendMessage(plugin.cfg().prefix() + "§cThat opponent has no technique."); return; }

        PlayerProfile prof = plugin.data().get(p.getUniqueId());
        int currentCharges = prof.copyCharges.getOrDefault(techId, 0);
        prof.copyCharges.put(techId, currentCharges + 5);
        if (prof.copiedTechniqueId == null) prof.copiedTechniqueId = techId;
        plugin.data().save(p.getUniqueId());

        hand.setAmount(hand.getAmount() - 1);
        if (hand.getAmount() <= 0) p.getInventory().setItemInMainHand(null);
        e.setCancelled(true);

        Technique t = plugin.techniques().get(techId);
        String displayName = t != null ? t.displayName() : techId;
        int totalCharges = prof.copyCharges.get(techId);
        p.sendMessage(plugin.cfg().prefix() + "§aAbsorbed §f" + displayName + "§a via Rika. §7(Charges: " + totalCharges + ")");
        p.playSound(p.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.8f, 1.4f);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRikaDamage(EntityDamageEvent e) {
        if (!rika.isRikaEntity(e.getEntity())) return;
        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRikaHitByEntity(EntityDamageByEntityEvent e) {
        if (!rika.isRikaEntity(e.getEntity())) return;
        e.setCancelled(true);
    }
}