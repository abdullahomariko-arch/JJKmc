package me.axebanz.jJK;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * Listens for key-press events and triggers bound abilities via KeybindManager.
 *
 * Key mappings:
 *   F           → PlayerSwapHandItemsEvent
 *   Q           → PlayerDropItemEvent
 *   SHIFT       → PlayerToggleSneakEvent (sneak start / end)
 *   RIGHT_CLICK → PlayerInteractEvent (right-click, empty hand)
 *   LEFT_CLICK  → PlayerInteractEvent (left-click, empty hand)
 */
public final class KeybindListener implements Listener {

    private final JJKCursedToolsPlugin plugin;
    private final KeybindManager keybindManager;

    public KeybindListener(JJKCursedToolsPlugin plugin, KeybindManager keybindManager) {
        this.plugin = plugin;
        this.keybindManager = keybindManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        keybindManager.loadFromPdc(e.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        keybindManager.saveToPdc(e.getPlayer());
        keybindManager.onQuit(e.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSwapHand(PlayerSwapHandItemsEvent e) {
        Player p = e.getPlayer();
        String ability = keybindManager.onKeyPress(p, "F");
        if (ability == null) return;

        e.setCancelled(true);

        KeybindManager.KeypressResult result = keybindManager.onKeyRelease(p, "F");
        if (result != null) executeAbility(p, result.ability, result.maxOutput);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDropItem(PlayerDropItemEvent e) {
        Player p = e.getPlayer();
        String ability = keybindManager.onKeyPress(p, "Q");
        if (ability == null) return;

        e.setCancelled(true);

        KeybindManager.KeypressResult result = keybindManager.onKeyRelease(p, "Q");
        if (result != null) executeAbility(p, result.ability, result.maxOutput);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onSneak(PlayerToggleSneakEvent e) {
        Player p = e.getPlayer();

        if (e.isSneaking()) {
            String ability = keybindManager.onKeyPress(p, "SHIFT");

            if (ability != null && isBlastAbility(ability)) {
                EnergyDischargeManager ed = plugin.energyDischarge();
                if (ed != null) ed.startBlastCharge(p);
            }
        } else {
            KeybindManager.KeypressResult result = keybindManager.onKeyRelease(p, "SHIFT");
            if (result != null) {
                if (isBlastAbility(result.ability)) {
                    EnergyDischargeManager ed = plugin.energyDischarge();
                    if (ed != null) ed.releaseBlastCharge(p);
                } else {
                    executeAbility(p, result.ability, result.maxOutput);
                }
            }
        }
    }

    private boolean isBlastAbility(String ability) {
        String lower = ability.toLowerCase(java.util.Locale.ROOT);
        return lower.equals("blast") || lower.equals("granite_blast");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;

        Player p = e.getPlayer();
        Action action = e.getAction();

        Material inHand = p.getInventory().getItemInMainHand().getType();
        if (inHand != Material.AIR) return;

        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            keybindManager.onKeyPress(p, "RIGHT_CLICK");
            KeybindManager.KeypressResult result = keybindManager.onKeyRelease(p, "RIGHT_CLICK");
            if (result != null) executeAbility(p, result.ability, result.maxOutput);
        } else if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            keybindManager.onKeyPress(p, "LEFT_CLICK");
            KeybindManager.KeypressResult result = keybindManager.onKeyRelease(p, "LEFT_CLICK");
            if (result != null) executeAbility(p, result.ability, result.maxOutput);
        }
    }

    private void executeAbility(Player p, String ability, boolean maxOutput) {
        LimitlessManager limitless = plugin.limitless();

        switch (ability.toLowerCase(java.util.Locale.ROOT)) {
            case "infinity" -> {
                if (limitless != null) limitless.toggleInfinity(p);
            }
            case "blue" -> {
                if (limitless != null) {
                    if (maxOutput) {
                        p.sendMessage(plugin.cfg().prefix() + "§bMaximum Output: Blue reached");
                        p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_BEACON_ACTIVATE, 0.7f, 1.8f);
                        p.sendActionBar(net.kyori.adventure.text.Component.text("Maximum Output: Blue reached"));
                        limitless.castBlueMax(p);
                    } else {
                        limitless.castBlue(p);
                    }
                }
            }
            case "red" -> {
                if (limitless != null) {
                    if (maxOutput) {
                        p.sendMessage(plugin.cfg().prefix() + "§cMaximum Output: Red reached");
                        p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_BEACON_ACTIVATE, 0.7f, 1.2f);
                        p.sendActionBar(net.kyori.adventure.text.Component.text("Maximum Output: Red reached"));
                        limitless.castRedMax(p);
                    } else {
                        limitless.castRed(p);
                    }
                }
            }
            case "purple" -> {
                if (limitless != null) limitless.castHollowPurple(p);
            }
            case "void", "infinitevoid" -> {
                if (limitless != null) limitless.castInfiniteVoid(p);
            }
            case "blast", "granite_blast" -> {
                // Granite Blast is handled ONLY by SHIFT press/release lifecycle.
                // Do not toggle here.
            }
            case "tracking", "tracking_beam" -> {
                EnergyDischargeManager ed = plugin.energyDischarge();
                if (ed != null) ed.startTrackingCharge(p);
            }
            default -> p.sendMessage(plugin.cfg().prefix() + "§cUnknown ability: §f" + ability);
        }
    }
}