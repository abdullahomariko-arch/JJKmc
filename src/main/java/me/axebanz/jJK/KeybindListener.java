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
 *   SHIFT       → PlayerToggleSneakEvent (sneak start)
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

    // ───────────────────────── Join / Quit ─────────────────────────

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        keybindManager.loadFromPdc(e.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        keybindManager.saveToPdc(e.getPlayer());
        keybindManager.onQuit(e.getPlayer().getUniqueId());
    }

    // ───────────────────────── F Key ─────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSwapHand(PlayerSwapHandItemsEvent e) {
        Player p = e.getPlayer();
        KeybindManager.KeypressResult result = keybindManager.onKeyRelease(p, "F");
        if (result == null) return;
        e.setCancelled(true);
        executeAbility(p, result.ability, result.maxOutput);
    }

    // ───────────────────────── Q Key ─────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDropItem(PlayerDropItemEvent e) {
        Player p = e.getPlayer();
        KeybindManager.KeypressResult result = keybindManager.onKeyRelease(p, "Q");
        if (result == null) return;
        e.setCancelled(true);
        executeAbility(p, result.ability, result.maxOutput);
    }

    // ───────────────────────── SHIFT Key ─────────────────────────

    @EventHandler(priority = EventPriority.HIGH)
    public void onSneak(PlayerToggleSneakEvent e) {
        if (!e.isSneaking()) return; // only trigger on sneak START
        Player p = e.getPlayer();
        KeybindManager.KeypressResult result = keybindManager.onKeyRelease(p, "SHIFT");
        if (result == null) return;
        executeAbility(p, result.ability, result.maxOutput);
    }

    // ───────────────────────── Right / Left Click ─────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        // Only main hand to avoid duplicate events
        if (e.getHand() != EquipmentSlot.HAND) return;

        Player p = e.getPlayer();
        Action action = e.getAction();

        // Only fire when holding nothing (empty hand) or air
        Material inHand = p.getInventory().getItemInMainHand().getType();
        if (inHand != Material.AIR) return;

        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            KeybindManager.KeypressResult result = keybindManager.onKeyRelease(p, "RIGHT_CLICK");
            if (result != null) {
                executeAbility(p, result.ability, result.maxOutput);
            }
        } else if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            KeybindManager.KeypressResult result = keybindManager.onKeyRelease(p, "LEFT_CLICK");
            if (result != null) {
                executeAbility(p, result.ability, result.maxOutput);
            }
        }
    }

    // ───────────────────────── Ability Dispatch ─────────────────────────

    /**
     * Dispatches the ability execution based on the bound ability string.
     * Supports Limitless abilities and can be extended for other techniques.
     */
    private void executeAbility(Player p, String ability, boolean maxOutput) {
        LimitlessManager limitless = plugin.limitless();

        switch (ability.toLowerCase()) {
            case "infinity" -> {
                if (limitless != null) limitless.toggleInfinity(p);
            }
            case "blue" -> {
                if (limitless != null) {
                    if (maxOutput) limitless.castBlueMax(p);
                    else limitless.castBlue(p);
                }
            }
            case "red" -> {
                if (limitless != null) {
                    if (maxOutput) limitless.castRedMax(p);
                    else limitless.castRed(p);
                }
            }
            case "bluemax", "blue_max" -> {
                if (limitless != null) limitless.castBlueMax(p);
            }
            case "redmax", "red_max" -> {
                if (limitless != null) limitless.castRedMax(p);
            }
            case "purple" -> {
                if (limitless != null) limitless.castHollowPurple(p);
            }
            case "nuke" -> {
                if (limitless != null) limitless.castNuke(p);
            }
            case "void", "infinitevoid" -> {
                if (limitless != null) limitless.castInfiniteVoid(p);
            }
            default -> p.sendMessage(plugin.cfg().prefix() + "§cUnknown ability: §f" + ability);
        }
    }
}
