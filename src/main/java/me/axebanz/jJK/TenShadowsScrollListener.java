package me.axebanz.jJK;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.block.Action;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TenShadowsScrollListener implements Listener {

    private final JJKCursedToolsPlugin plugin;
    private final TenShadowsManager manager;
    private final TenShadowsSelectionUI ui;

    /** Tracks ritual countdown tasks so they can be cancelled */
    private final Map<UUID, Integer> ritualCountdownTaskIds = new ConcurrentHashMap<>();

    public TenShadowsScrollListener(JJKCursedToolsPlugin plugin, TenShadowsManager manager) {
        this.plugin = plugin;
        this.manager = manager;
        this.ui = new TenShadowsSelectionUI(plugin);
    }

    /**
     * Shift + Scroll cycles through ALL shikigami (locked and unlocked) one at a time.
     * While Shift is held and scrolling, the actionbar shows ONLY the selected shikigami name.
     */
    @EventHandler
    public void onSlotChange(PlayerItemHeldEvent e) {
        Player p = e.getPlayer();
        if (!p.isSneaking()) return;

        String assignedId = plugin.techniqueManager().getAssignedId(p.getUniqueId());
        if (!"ten_shadows".equalsIgnoreCase(assignedId)) return;

        TenShadowsProfile prof = manager.getProfile(p.getUniqueId());

        // Allow scrolling even if a shikigami is summoned (just for browsing)
        // But do NOT update hotbar slot — cancel the event
        int prev = e.getPreviousSlot();
        int next = e.getNewSlot();

        int diff = next - prev;
        boolean scrollForward = (diff == -8 || (diff > 0 && diff < 5));

        if (scrollForward) {
            ui.scrollNext(p, prof);
        } else {
            ui.scrollPrev(p, prof);
        }

        e.setCancelled(true);

        // Cancel any pending ritual countdown since player scrolled
        cancelRitualCountdown(p.getUniqueId());
    }

    /**
     * When player releases Shift, stop showing the scroll UI on actionbar.
     */
    @EventHandler
    public void onToggleSneak(PlayerToggleSneakEvent e) {
        if (e.isSneaking()) return; // Only care about when they STOP sneaking

        Player p = e.getPlayer();
        String assignedId = plugin.techniqueManager().getAssignedId(p.getUniqueId());
        if (!"ten_shadows".equalsIgnoreCase(assignedId)) return;

        // Cancel any pending ritual countdown when Shift is released
        cancelRitualCountdown(p.getUniqueId());
    }

    /**
     * Sneak + right-click:
     *   - If a shikigami is summoned → dismiss it
     *   - If selected shikigami is UNLOCKED → summon it
     *   - If selected shikigami is LOCKED → start a ritual countdown (5...4...3...2...1...)
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

        // Don't double-trigger if countdown already running
        if (ritualCountdownTaskIds.containsKey(p.getUniqueId())) return;

        ShikigamiType selected = ui.getSelected(prof);
        if (selected == null) {
            p.sendMessage(plugin.cfg().prefix() + "§cNo shikigami available. Sneak + scroll to select.");
            return;
        }

        // If unlocked, summon directly
        if (prof.isUnlocked(selected)) {
            manager.trySummon(p, selected);
            return;
        }

        // If locked and requires ritual, start ritual countdown
        if (selected.requiresRitual() && !prof.isDestroyed(selected)) {
            startRitualCountdown(p, selected);
        } else {
            p.sendMessage(plugin.cfg().prefix() + "§c" + selected.displayName() + " §ccannot be summoned or started from here.");
        }
    }

    // ---- Ritual Countdown ----

    private void startRitualCountdown(Player p, ShikigamiType type) {
        UUID uuid = p.getUniqueId();
        final int[] countdown = {5};

        p.sendActionBar("§c§lRitual starting in §f§l" + countdown[0] + "...");
        p.sendMessage(plugin.cfg().prefix() + "§c§lRitual starting in §f§l5§c§l... (release Shift to cancel)");

        int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            Player online = Bukkit.getPlayer(uuid);
            if (online == null || !online.isOnline() || !online.isSneaking()) {
                cancelRitualCountdown(uuid);
                if (online != null) online.sendMessage(plugin.cfg().prefix() + "§7Ritual countdown cancelled.");
                return;
            }
            countdown[0]--;
            if (countdown[0] > 0) {
                online.sendActionBar("§c§lRitual starting in §f§l" + countdown[0] + "...");
            } else {
                online.sendActionBar("§c§l⚡ RITUAL START ⚡");
                cancelRitualCountdown(uuid);
                manager.startRitual(online, type);
            }
        }, 20L, 20L);

        ritualCountdownTaskIds.put(uuid, taskId);
    }

    private void cancelRitualCountdown(UUID uuid) {
        Integer taskId = ritualCountdownTaskIds.remove(uuid);
        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
    }
}