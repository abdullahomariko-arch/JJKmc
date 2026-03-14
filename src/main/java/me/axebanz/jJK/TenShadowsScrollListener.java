package me.axebanz.jJK;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.block.Action;

public final class TenShadowsScrollListener implements Listener {

    private final JJKCursedToolsPlugin plugin;
    private final TenShadowsManager manager;
    private final TenShadowsSelectionUI ui;

    public TenShadowsScrollListener(JJKCursedToolsPlugin plugin, TenShadowsManager manager) {
        this.plugin = plugin;
        this.manager = manager;
        this.ui = new TenShadowsSelectionUI(plugin);
    }

    /**
     * Scroll wheel changes hotbar slot — when sneaking with Ten Shadows,
     * intercept to scroll through shikigami selection.
     */
    @EventHandler
    public void onSlotChange(PlayerItemHeldEvent e) {
        Player p = e.getPlayer();
        if (!p.isSneaking()) return;

        String assignedId = plugin.techniqueManager().getAssignedId(p.getUniqueId());
        if (!"ten_shadows".equalsIgnoreCase(assignedId)) return;

        TenShadowsProfile prof = manager.getProfile(p.getUniqueId());

        // Don't intercept if a shikigami is already summoned
        if (prof.activeSummonId != null) return;

        // Determine scroll direction
        int prev = e.getPreviousSlot();
        int next = e.getNewSlot();

        // Handle wrap-around (8->0 = forward, 0->8 = backward)
        int diff = next - prev;
        if (diff == -8 || (diff > 0 && diff < 5)) {
            ui.scrollNext(p, prof);
        } else {
            ui.scrollPrev(p, prof);
        }

        e.setCancelled(true);
    }

    /**
     * Sneak + right-click air = summon selected shikigami
     */
    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player p = e.getPlayer();
        if (!p.isSneaking()) return;

        String assignedId = plugin.techniqueManager().getAssignedId(p.getUniqueId());
        if (!"ten_shadows".equalsIgnoreCase(assignedId)) return;

        TenShadowsProfile prof = manager.getProfile(p.getUniqueId());

        // If already summoned, dismiss on sneak + right click
        if (prof.activeSummonId != null) {
            manager.dismiss(p);
            return;
        }

        ShikigamiType selected = ui.getSelected(prof);
        if (selected == null) {
            p.sendMessage(plugin.cfg().prefix() + "§cNo shikigami available to summon.");
            return;
        }

        manager.trySummon(p, selected);
    }
}