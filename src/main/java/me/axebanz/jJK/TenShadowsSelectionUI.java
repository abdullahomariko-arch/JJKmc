package me.axebanz.jJK;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Scroll-wheel based shikigami selection UI.
 * Player holds Shift and scrolls to cycle through ALL shikigami (locked + unlocked).
 * Locked shikigami show a 🔒 symbol. Unlocked shikigami are highlighted.
 * When Shift is released, the UI returns to normal actionbar display.
 */
public final class TenShadowsSelectionUI {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final JJKCursedToolsPlugin plugin;

    public TenShadowsSelectionUI(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Render the selection wheel on the actionbar.
     * Shows all shikigami; locked ones show 🔒 and strikethrough.
     */
    public void showSelectionWheel(Player p, TenShadowsProfile prof) {
        List<ShikigamiType> all = prof.getScrollWheelShikigami();
        if (all.isEmpty()) {
            sendMini(p, "<dark_gray>No shikigami available.</dark_gray>");
            return;
        }

        int index = Math.floorMod(prof.scrollIndex, all.size());

        StringBuilder sb = new StringBuilder();
        sb.append("<dark_gray>◀ </dark_gray>");

        for (int i = 0; i < all.size(); i++) {
            ShikigamiType type = all.get(i);
            boolean locked = !prof.isUnlocked(type);
            String clean = stripLegacy(type.displayName());

            if (i == index) {
                if (locked) {
                    sb.append("<red><bold>🔒 <strikethrough>").append(clean).append("</strikethrough></bold></red>");
                } else {
                    sb.append("<white><bold>[ ").append(clean).append(" ]</bold></white>");
                }
            } else {
                if (locked) {
                    sb.append("<dark_red>🔒 <strikethrough>").append(clean).append("</strikethrough></dark_red>");
                } else {
                    sb.append("<dark_gray>").append(clean).append("</dark_gray>");
                }
            }

            if (i < all.size() - 1) {
                sb.append("  <dark_gray>|</dark_gray>  ");
            }
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
     * Scroll forward (one step).
     */
    public void scrollNext(Player p, TenShadowsProfile prof) {
        prof.scrollIndex++;
        showSelectionWheel(p, prof);
    }

    /**
     * Scroll backward (one step).
     */
    public void scrollPrev(Player p, TenShadowsProfile prof) {
        prof.scrollIndex--;
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