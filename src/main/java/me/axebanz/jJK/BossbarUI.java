package me.axebanz.jJK;

import org.bukkit.Bukkit;
import org.bukkit.boss.*;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BossbarUI {

    private final JJKCursedToolsPlugin plugin;
    private int taskId = -1;

    private final Map<UUID, BossBar> ceBars = new ConcurrentHashMap<>();
    private final Map<UUID, BossBar> cooldownBars = new ConcurrentHashMap<>();
    private final Map<UUID, BossBar> nullifiedBars = new ConcurrentHashMap<>();
    private final Map<UUID, BossBar> isohReapplyBars = new ConcurrentHashMap<>();

    public BossbarUI(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (taskId != -1) Bukkit.getScheduler().cancelTask(taskId);
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            long now = System.currentTimeMillis();
            for (Player p : Bukkit.getOnlinePlayers()) {
                UUID u = p.getUniqueId();
                updateCeBar(p);
                updateNullifiedBar(p, u, now);
                updateIsohReapplyBar(p, u, now);
                updateCooldownBar(p, u, now);
            }
        }, 20L, 20L);
    }

    public void attachPlayer(Player p) {
        if (plugin.cfg().ceBossbarEnabled()) {
            ceBars.computeIfAbsent(p.getUniqueId(), k -> Bukkit.createBossBar("", BarColor.PURPLE, BarStyle.SEGMENTED_20))
                    .addPlayer(p);
        }
    }

    public void detachPlayer(Player p) {
        UUID u = p.getUniqueId();
        removeBar(ceBars.remove(u), p);
        removeBar(cooldownBars.remove(u), p);
        removeBar(nullifiedBars.remove(u), p);
        removeBar(isohReapplyBars.remove(u), p);
    }

    private void removeBar(BossBar bar, Player p) {
        if (bar != null) bar.removePlayer(p);
    }

    private void updateCeBar(Player p) {
        if (!plugin.cfg().ceBossbarEnabled()) return;

        BossBar bar = ceBars.computeIfAbsent(p.getUniqueId(), k -> Bukkit.createBossBar("", BarColor.PURPLE, BarStyle.SEGMENTED_20));
        if (!bar.getPlayers().contains(p)) bar.addPlayer(p);

        int max = plugin.ce().max(p.getUniqueId());
        int val = plugin.ce().get(p.getUniqueId());
        val = Math.max(0, Math.min(max, val));

        int segments = 20;
        double pct = (max <= 0) ? 0.0 : (val / (double) max);
        int filledSegments = (int) Math.round(pct * segments);

        boolean copyActive = plugin.copy() != null && plugin.copy().canUseCopy(p);
        boolean projectionActive = plugin.projectionManager() != null
                && "projection".equalsIgnoreCase(plugin.techniqueManager().getAssignedId(p.getUniqueId()));

        String filled = plugin.cfg().c().getString("ui.cursedEnergyBossbar.filledChar", "■");
        String empty = plugin.cfg().c().getString("ui.cursedEnergyBossbar.emptyChar", "□");

        String colorCode = projectionActive ? "§9" : (copyActive ? "§d" : "§d");

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < segments; i++) {
            sb.append(i < filledSegments ? colorCode + filled : "§7" + empty);
        }

        String titleTpl = plugin.cfg().c().getString("ui.cursedEnergyBossbar.title",
                "§dCursed Energy §8|§r {bar} §7({value}/{max})");

        bar.setTitle(titleTpl
                .replace("{bar}", sb.toString())
                .replace("{value}", String.valueOf(val))
                .replace("{max}", String.valueOf(max)));

        bar.setStyle(BarStyle.SEGMENTED_20);
        bar.setProgress(Math.max(0.0, Math.min(1.0, pct)));
        BarColor barColor = projectionActive ? BarColor.BLUE : (copyActive ? BarColor.PINK : BarColor.PURPLE);
        bar.setColor(barColor);
        bar.setVisible(true);
    }

    private void updateCooldownBar(Player p, UUID u, long now) {
        BossBar bar = cooldownBars.get(u);
        if (bar == null) return;
        if (!bar.isVisible()) return;
    }

    public void showCooldownBossbar(Player p, String title, BarColor color, long durationSeconds) {
        BossBar bar = cooldownBars.computeIfAbsent(p.getUniqueId(), k -> Bukkit.createBossBar("", color, BarStyle.SOLID));
        if (!bar.getPlayers().contains(p)) bar.addPlayer(p);
        long endsAt = System.currentTimeMillis() + durationSeconds * 1000L;
        bar.setVisible(true); bar.setColor(color); bar.setStyle(BarStyle.SOLID);
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            long rem = Math.max(0, (endsAt - System.currentTimeMillis()) / 1000L);
            if (rem <= 0) { bar.setVisible(false); task.cancel(); return; }
            bar.setTitle(title.replace("{time}", TimeFmt.mmss(rem)));
            bar.setProgress(Math.max(0.0, Math.min(1.0, rem / (double) durationSeconds)));
        }, 0L, 20L);
    }

    private void updateNullifiedBar(Player p, UUID u, long now) {
        boolean nullified = plugin.nullify().isNullified(u);
        BossBar bar = nullifiedBars.get(u);
        if (!nullified) { if (bar != null) bar.setVisible(false); return; }
        long rem = plugin.nullify().remainingSeconds(u);
        String hex = plugin.techniqueManager().techniqueColorHex(u);
        String legacy = HexColor.legacyFromHex(hex);
        bar = nullifiedBars.computeIfAbsent(u, k -> Bukkit.createBossBar("", BarColor.RED, BarStyle.SOLID));
        if (!bar.getPlayers().contains(p)) bar.addPlayer(p);
        String titleTpl = plugin.cfg().c().getString("ui.nullified.bossbarTitle", "§cNULLIFIED §8|§r {time}");
        bar.setTitle(legacy + titleTpl.replace("{time}", TimeFmt.mmss(rem)));
        bar.setColor(BarColor.RED);
        bar.setProgress(Math.max(0.0, Math.min(1.0, rem / 600.0)));
        bar.setVisible(true);
    }

    private void updateIsohReapplyBar(Player p, UUID u, long now) {
        PlayerProfile prof = plugin.data().get(u);
        long until = prof.isohReapplyUntilMs;
        BossBar bar = isohReapplyBars.get(u);
        if (until <= now) { if (bar != null) bar.setVisible(false); return; }
        long rem = Math.max(0, (until - now) / 1000L);
        String colorHex = "#FFA500";
        if (prof.isohReapplyTargetUuid != null) {
            try {
                UUID target = UUID.fromString(prof.isohReapplyTargetUuid);
                colorHex = plugin.nullify().targetTechniqueColor(target);
            } catch (Exception ignored) {}
        }
        String legacy = HexColor.legacyFromHex(colorHex);
        bar = isohReapplyBars.computeIfAbsent(u, k -> Bukkit.createBossBar("", BarColor.YELLOW, BarStyle.SOLID));
        if (!bar.getPlayers().contains(p)) bar.addPlayer(p);
        bar.setTitle(legacy + "ISOH Re-apply Window §8|§r " + TimeFmt.mmss(rem));
        bar.setColor(BarColor.YELLOW);
        bar.setProgress(Math.max(0.0, Math.min(1.0, rem / 600.0)));
        bar.setVisible(true);
    }

    public void showNullifiedTitle(Player p) {
        String title = plugin.cfg().c().getString("ui.nullified.titleMain", "§cTECHNIQUE NULLIFIED");
        String sub = plugin.cfg().c().getString("ui.nullified.subtitle", "§7Your technique is sealed.");
        p.sendTitle(title, sub, 10, 40, 10);
    }
}