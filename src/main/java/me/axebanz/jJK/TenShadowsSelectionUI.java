package me.axebanz.jJK;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Scroll-wheel based shikigami selection UI.
 * Player holds Shift and scrolls to cycle through ALL shikigami (locked + unlocked).
 * Shows ONE shikigami name at a time, centered in the action bar.
 * Locked shikigami show a 🔒 symbol. Unlocked shikigami are highlighted.
 * When Shift is released, the UI returns to normal actionbar display.
 *
 * Totality selector: if the player hovers on Nue for 3+ seconds AND has Nue Totality unlocked,
 * "§6Nue: Totality" becomes the secondary display option (pressing summon uses it).
 */
public final class TenShadowsSelectionUI {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final long TOTALITY_HOVER_MS = 3000L;

    private final JJKCursedToolsPlugin plugin;

    public TenShadowsSelectionUI(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Render the selection wheel on the actionbar.
     * Shows only the currently selected shikigami name.
     */
    public void showSelectionWheel(Player p, TenShadowsProfile prof) {
        List<ShikigamiType> all = prof.getScrollWheelShikigami();
        if (all.isEmpty()) {
            sendMini(p, "<dark_gray>No shikigami available.</dark_gray>");
            return;
        }

        int index = Math.floorMod(prof.scrollIndex, all.size());
        ShikigamiType type = all.get(index);
        boolean locked = !prof.isUnlocked(type);
        String clean = stripLegacy(type.displayName());

        // Check if Totality selector is active (hovered 3+ seconds on a shikigami that has a Totality)
        ShikigamiType totality = getTotalityFor(type, prof);
        boolean totalityActive = totality != null && prof.scrollHoverSinceMs > 0
                && (System.currentTimeMillis() - prof.scrollHoverSinceMs) >= TOTALITY_HOVER_MS;

        StringBuilder sb = new StringBuilder();
        if (locked) {
            sb.append("<dark_red>🔒 <strikethrough>").append(clean).append("</strikethrough></dark_red>");
        } else if (totalityActive) {
            String totalityName = stripLegacy(totality.displayName());
            // Show both: base shikigami dimmed, totality highlighted
            sb.append("<gray>").append(clean).append("</gray>")
              .append("  <gold><bold>✦ ").append(totalityName).append(" ✦</bold></gold>");
        } else {
            sb.append("<white><bold>[ ").append(clean).append(" ]</bold></white>");
        }

        sendMini(p, sb.toString());
    }

    /**
     * Get the currently selected shikigami type.
     * If the Totality selector is active, returns the Totality type instead.
     */
    public ShikigamiType getSelected(TenShadowsProfile prof) {
        List<ShikigamiType> all = prof.getScrollWheelShikigami();
        if (all.isEmpty()) return null;
        int index = Math.floorMod(prof.scrollIndex, all.size());
        ShikigamiType type = all.get(index);

        // If totality is active, return the totality
        ShikigamiType totality = getTotalityFor(type, prof);
        if (totality != null && prof.scrollHoverSinceMs > 0
                && (System.currentTimeMillis() - prof.scrollHoverSinceMs) >= TOTALITY_HOVER_MS) {
            return totality;
        }
        return type;
    }

    /**
     * Get the base (non-totality) selected shikigami.
     */
    public ShikigamiType getBaseSelected(TenShadowsProfile prof) {
        List<ShikigamiType> all = prof.getScrollWheelShikigami();
        if (all.isEmpty()) return null;
        int index = Math.floorMod(prof.scrollIndex, all.size());
        return all.get(index);
    }

    /**
     * Scroll forward (one step). Resets the totality hover timer.
     */
    public void scrollNext(Player p, TenShadowsProfile prof) {
        prof.scrollIndex++;
        prof.scrollHoverSinceMs = System.currentTimeMillis();
        showSelectionWheel(p, prof);
    }

    /**
     * Scroll backward (one step). Resets the totality hover timer.
     */
    public void scrollPrev(Player p, TenShadowsProfile prof) {
        prof.scrollIndex--;
        prof.scrollHoverSinceMs = System.currentTimeMillis();
        showSelectionWheel(p, prof);
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

    /**
     * Returns the totality version of a shikigami if it exists and the player has it unlocked.
     * Only Nue → NUE_TOTALITY is currently supported.
     */
    private ShikigamiType getTotalityFor(ShikigamiType base, TenShadowsProfile prof) {
        if (base == ShikigamiType.NUE && prof.isUnlocked(ShikigamiType.NUE_TOTALITY)) {
            return ShikigamiType.NUE_TOTALITY;
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