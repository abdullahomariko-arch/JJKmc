package me.axebanz.jJK;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Scroll-wheel based shikigami selection UI.
 * Player holds Shift and scrolls to cycle through shikigami ONE at a time.
 * Shows ONE name centered on the action bar, with 🔒 for locked ones.
 * After 3 seconds hovering on a totality-capable shikigami, shows the totality option.
 */
public final class TenShadowsSelectionUI {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    /** How long (ms) a player must hover on a selection before the totality hint appears */
    private static final long TOTALITY_HINT_DELAY_MS = 3000L;

    /** Tracks when each player started hovering on their current selection */
    private final Map<UUID, Long> hoverStartMs = new ConcurrentHashMap<>();

    private final JJKCursedToolsPlugin plugin;

    public TenShadowsSelectionUI(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Render the selection wheel on the actionbar — ONE name at a time, centered.
     * Locked shikigami show a 🔒 prefix.
     * If the player has hovered on a Totality-eligible shikigami for 3+ seconds, show the totality arrow.
     */
    public void showSelectionWheel(Player p, TenShadowsProfile prof) {
        List<ShikigamiType> all = prof.getScrollWheelShikigami();
        if (all.isEmpty()) {
            sendMini(p, "<dark_gray>No shikigami available.</dark_gray>");
            return;
        }

        int index = Math.floorMod(prof.scrollIndex, all.size());
        ShikigamiType selected = all.get(index);
        boolean locked = !prof.isUnlocked(selected);
        String clean = stripLegacy(selected.displayName());

        // Check totality hint (3+ seconds hover)
        long now = System.currentTimeMillis();
        UUID uid = p.getUniqueId();
        long hoverStart = hoverStartMs.getOrDefault(uid, now);
        boolean showTotality = !locked && (now - hoverStart >= TOTALITY_HINT_DELAY_MS);

        String totalityLabel = null;
        if (showTotality) {
            totalityLabel = getTotalityLabel(selected, prof);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<dark_gray>◀ </dark_gray>");

        if (locked) {
            sb.append("<red><bold>🔒 <strikethrough>").append(clean).append("</strikethrough></bold></red>");
        } else if (totalityLabel != null) {
            // Show "Name → Totality Name"
            sb.append("<white><bold>[ ").append(clean).append(" ]</bold></white>")
              .append("<dark_gray> → </dark_gray>")
              .append("<gold><bold>").append(totalityLabel).append("</bold></gold>");
        } else {
            sb.append("<white><bold>[ ").append(clean).append(" ]</bold></white>");
        }

        sb.append("<dark_gray> ▶</dark_gray>");

        sendMini(p, sb.toString());
    }

    /**
     * Get the currently selected shikigami type.
     */
    public ShikigamiType getSelected(TenShadowsProfile prof) {
        List<ShikigamiType> all = prof.getScrollWheelShikigami();
        if (all.isEmpty()) return null;
        int index = Math.floorMod(prof.scrollIndex, all.size());
        return all.get(index);
    }

    /**
     * Returns the totality version to summon if the player has hovered long enough.
     * Returns null if no totality transition applies.
     */
    public ShikigamiType getTotalityOverride(Player p, TenShadowsProfile prof) {
        long now = System.currentTimeMillis();
        UUID uid = p.getUniqueId();
        long hoverStart = hoverStartMs.getOrDefault(uid, now);
        if (now - hoverStart < TOTALITY_HINT_DELAY_MS) return null;

        ShikigamiType selected = getSelected(prof);
        if (selected == null) return null;

        if (selected == ShikigamiType.NUE && prof.isUnlocked(ShikigamiType.NUE_TOTALITY)) {
            return ShikigamiType.NUE_TOTALITY;
        }
        if (selected == ShikigamiType.TOAD && ShikigamiType.isToadTotalityAvailable(prof)) {
            return ShikigamiType.TOAD_TOTALITY;
        }
        return null;
    }

    /**
     * Scroll forward (one step). Resets the hover timer.
     */
    public void scrollNext(Player p, TenShadowsProfile prof) {
        prof.scrollIndex++;
        hoverStartMs.put(p.getUniqueId(), System.currentTimeMillis());
        showSelectionWheel(p, prof);
    }

    /**
     * Scroll backward (one step). Resets the hover timer.
     */
    public void scrollPrev(Player p, TenShadowsProfile prof) {
        prof.scrollIndex--;
        hoverStartMs.put(p.getUniqueId(), System.currentTimeMillis());
        showSelectionWheel(p, prof);
    }

    /**
     * Clear hover timer when a player stops scrolling or leaves.
     */
    public void clearHover(UUID playerUuid) {
        hoverStartMs.remove(playerUuid);
    }

    /**
     * Show the full shikigami list status to the player.
     */
    public void showStatusList(Player p, TenShadowsProfile prof) {
        p.sendMessage(plugin.cfg().prefix() + "§8§l═══ Ten Shadows ═══");
        for (ShikigamiType type : ShikigamiType.values()) {
            ShikigamiState state = prof.getState(type);
            String stateStr = switch (state) {
                case LOCKED -> "§7✖ Locked";
                case UNLOCKED -> "§a✔ Available";
                case ACTIVE -> "§b★ Summoned";
                case DESTROYED -> "§4✖ Destroyed";
                case FUSED_UNLOCKED -> "§6✦ Fused";
            };
            p.sendMessage("  " + type.displayName() + " §8— " + stateStr);
        }
    }

    // ---- Helpers ----

    /** Returns the display label for the totality form of a shikigami, or null if not applicable. */
    private String getTotalityLabel(ShikigamiType type, TenShadowsProfile prof) {
        if (type == ShikigamiType.NUE && prof.isUnlocked(ShikigamiType.NUE_TOTALITY)) {
            return stripLegacy(ShikigamiType.NUE_TOTALITY.displayName());
        }
        if (type == ShikigamiType.TOAD && ShikigamiType.isToadTotalityAvailable(prof)) {
            return stripLegacy(ShikigamiType.TOAD_TOTALITY.displayName());
        }
        return null;
    }

    private void sendMini(Player p, String miniMessage) {
        try {
            Component c = MINI.deserialize(miniMessage);
            p.sendActionBar(c);
        } catch (Exception e) {
            p.sendActionBar(miniMessage);
        }
    }

    private String stripLegacy(String input) {
        if (input == null) return "";
        return input.replaceAll("§[0-9a-fk-or]", "");
    }
}