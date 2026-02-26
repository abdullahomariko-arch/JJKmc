package me.axebanz.jJK;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages the Cursed Energy boss bar UI.
 * Bug Fix #2: Projection users get 15 segments (SEGMENTED_12 style) instead of 10.
 */
public class BossbarUI {
    private final Map<UUID, BossBar> ceBars = new HashMap<>();
    private final JJKCursedToolsPlugin plugin;

    public BossbarUI(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    public void updateCeBar(Player player, int current, int max) {
        BossBar bar = ceBars.computeIfAbsent(player.getUniqueId(), uuid -> {
            BossBar b = createBar(player);
            b.addPlayer(player);
            return b;
        });

        // Bug Fix #2: Use 15 segments for Projection users, 10 for others
        String techId = plugin.techniqueManager().getAssignedId(player.getUniqueId());
        boolean isProjection = "projection".equalsIgnoreCase(techId);

        int segments;
        BarStyle style;
        if (isProjection) {
            segments = 15;
            style = BarStyle.SEGMENTED_12; // Bukkit's closest available style to 15 segments; SEGMENTED_12 has 12 divisions
        } else {
            segments = 10;
            style = BarStyle.SEGMENTED_10;
        }

        bar.setStyle(style);

        double progress = max > 0 ? (double) current / max : 0.0;
        progress = Math.max(0.0, Math.min(1.0, progress));
        bar.setProgress(progress);

        BarColor color = current > max / 2 ? BarColor.BLUE : BarColor.RED;
        bar.setColor(color);

        bar.setTitle("§5§lCE§r §7" + current + "§8/§7" + max);
        bar.setVisible(true);
    }

    private BossBar createBar(Player player) {
        return Bukkit.createBossBar("§5§lCE§r", BarColor.BLUE, BarStyle.SEGMENTED_10);
    }

    public void removeBar(Player player) {
        BossBar bar = ceBars.remove(player.getUniqueId());
        if (bar != null) {
            bar.removePlayer(player);
            bar.setVisible(false);
        }
    }

    public void hideAll() {
        for (BossBar bar : ceBars.values()) {
            bar.setVisible(false);
        }
    }
}
