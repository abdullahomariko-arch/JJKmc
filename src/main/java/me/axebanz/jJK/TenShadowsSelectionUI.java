package me.axebanz.jJK;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

/**
 * Scroll-wheel based shikigami selection UI.
 * Player holds a key (sneak + scroll) to cycle through available shikigami,
 * then releases to summon.
 *
 * Shown via actionbar — uses Nexo glyph tags for icons.
 */
public final class TenShadowsSelectionUI {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final JJKCursedToolsPlugin plugin;

    public TenShadowsSelectionUI(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Render the selection wheel on the actionbar.
     */
    public void showSelectionWheel(Player p, TenShadowsProfile prof) {
        List<ShikigamiType> available = prof.getSummonableShikigami();
        if (available.isEmpty()) {
            sendMini(p, "<dark_gray>No shikigami available.</dark_gray>");
            return;
        }

        int index = prof.scrollIndex % available.size();
        if (index < 0) index += available.size();

        StringBuilder sb = new StringBuilder();
        sb.append("<dark_gray>◀ </dark_gray>");

        for (int i = 0; i < available.size(); i++) {
            ShikigamiType type = available.get(i);
            String clean = stripLegacy(type.displayName());

            if (i == index) {
                sb.append("<white><bold>[ ").append(clean).append(" ]</bold></white>");
            } else {
                sb.append("<dark_gray>").append(clean).append("</dark_gray>");
            }

            if (i < available.size() - 1) {
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
        List<ShikigamiType> available = prof.getSummonableShikigami();
        if (available.isEmpty()) return null;
        int index = prof.scrollIndex % available.size();
        if (index < 0) index += available.size();
        return available.get(index);
    }

    /**
     * Scroll forward.
     */
    public void scrollNext(Player p, TenShadowsProfile prof) {
        prof.scrollIndex++;
        showSelectionWheel(p, prof);
    }

    /**
     * Scroll backward.
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