package me.axebanz.jJK;

import org.bukkit.entity.Player;

public class ActionbarUI {
    private final JJKCursedToolsPlugin plugin;

    public ActionbarUI(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    public void sendCeBar(Player player, int current, int max) {
        int bars = 20;
        int filled = (int) ((double) current / max * bars);
        StringBuilder sb = new StringBuilder("§5CE §7[");
        for (int i = 0; i < bars; i++) {
            sb.append(i < filled ? "§b|" : "§8|");
        }
        sb.append("§7] §b").append(current).append("§7/§b").append(max);
        player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                net.md_5.bungee.api.chat.TextComponent.fromLegacyText(sb.toString()));
    }
}
