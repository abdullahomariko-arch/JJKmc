package me.axebanz.jJK;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class ActionbarUI {

    private final JJKCursedToolsPlugin plugin;
    private int taskId = -1;

    private static final String NO_TECHNIQUE_GLYPH = "<glyph:technique_none>";
    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final Map<UUID, Map<String, ActionbarTimer>> timers = new ConcurrentHashMap<>();

    public ActionbarUI(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (taskId != -1) Bukkit.getScheduler().cancelTask(taskId);

        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!plugin.cfg().cooldownActionbarEnabled()) return;

            long now = System.currentTimeMillis();
            for (Player p : Bukkit.getOnlinePlayers()) {
                UUID u = p.getUniqueId();
                Map<String, ActionbarTimer> map = timers.get(u);

                // Build the technique icon prefix (MiniMessage format)
                String iconPrefix = buildTechniqueIcon(u);

                if (map == null || map.isEmpty()) {
                    sendMiniActionBar(p, iconPrefix);
                    continue;
                }

                map.values().removeIf(t -> t.endsAtMs <= now);
                if (map.isEmpty()) {
                    sendMiniActionBar(p, iconPrefix);
                    continue;
                }

                // If cursed speech timers exist, render 3 cooldowns at once
                if (map.containsKey("cursed_speech.plummet")
                        || map.containsKey("cursed_speech.nomove")
                        || map.containsKey("cursed_speech.explode")) {

                    String msg = renderCursedSpeechBar(map, now);
                    if (msg != null && !msg.isBlank()) {
                        sendMiniActionBar(p, iconPrefix + "  <gray>|</gray>  " + msg);
                        continue;
                    }
                }

                // Default: show chosen timer with icon prefix
                ActionbarTimer chosen = choose(map.values(), plugin.cfg().cooldownPreferShortest(), now);
                long remSec = Math.max(0, (chosen.endsAtMs - now) / 1000L);
                String msg = chosen.color + chosen.icon + " <white>" + TimeFmt.mmss(remSec) + "</white>";
                sendMiniActionBar(p, iconPrefix + "  <dark_gray>|</dark_gray>  " + msg);
            }
        }, 20L, 20L);
    }

    /**
     * Send an actionbar using MiniMessage so glyph tags render properly.
     */
    private void sendMiniActionBar(Player p, String miniMessageString) {
        try {
            Component component = MINI.deserialize(miniMessageString);
            p.sendActionBar(component);
        } catch (Exception e) {
            // Fallback to plain text if MiniMessage fails
            p.sendActionBar(miniMessageString);
        }
    }

    /**
     * Builds the technique icon string using Nexo glyph tags.
     */
    private String buildTechniqueIcon(UUID uuid) {
        Technique tech = plugin.techniqueManager().getAssigned(uuid);

        if (tech == null) {
            return NO_TECHNIQUE_GLYPH + "<shift:4><dark_gray>No Technique</dark_gray>";
        }

        String glyph = tech.glyphTag();
        String name = tech.displayName();

        // Strip legacy color codes from name for MiniMessage
        String cleanName = stripLegacyColors(name);

        // Check if nullified
        if (plugin.nullify().isNullified(uuid)) {
            long rem = plugin.nullify().remainingSeconds(uuid);
            return "<red>✖ <strikethrough>NULLIFIED</strikethrough></red> <dark_gray>(<white>" + TimeFmt.mmss(rem) + "</white>)</dark_gray>";
        }

        String hexColor = tech.hexColor();
        // Glyph uses original PNG colors, only the text gets colored
        return glyph + "<shift:4><" + hexColor + ">" + cleanName + "</" + hexColor + ">";
    }

    /**
     * Strip legacy § color codes so MiniMessage doesn't break.
     */
    private String stripLegacyColors(String input) {
        if (input == null) return "";
        return input.replaceAll("§[0-9a-fk-or]", "");
    }

    private String renderCursedSpeechBar(Map<String, ActionbarTimer> map, long now) {
        String a = one(map, "cursed_speech.plummet", "<red>Plummet</red>");
        String b = one(map, "cursed_speech.nomove", "<yellow>Don't Move</yellow>");
        String c = one(map, "cursed_speech.explode", "<gold>Explode</gold>");

        List<String> parts = new ArrayList<>();
        if (a != null) parts.add(a);
        if (b != null) parts.add(b);
        if (c != null) parts.add(c);

        if (parts.isEmpty()) return null;

        return String.join("   <dark_gray>|</dark_gray>   ", parts);
    }

    private String one(Map<String, ActionbarTimer> map, String key, String label) {
        ActionbarTimer t = map.get(key);
        if (t == null) return "<gray>{" + label + "<gray>: </gray><green>Ready</green><gray>}</gray>";

        long remSec = Math.max(0, (t.endsAtMs - System.currentTimeMillis()) / 1000L);
        return "<gray>{" + label + "<gray>: </gray><white>" + TimeFmt.mmss(remSec) + "</white><gray>}</gray>";
    }

    private ActionbarTimer choose(Collection<ActionbarTimer> values, boolean preferShortest, long now) {
        ActionbarTimer best = null;
        for (ActionbarTimer t : values) {
            if (best == null) { best = t; continue; }
            long remT = t.endsAtMs - now;
            long remB = best.endsAtMs - now;

            if (preferShortest) {
                if (remT < remB) best = t;
            } else {
                if (t.createdAtMs > best.createdAtMs) best = t;
            }
        }
        return best;
    }

    public void setTimer(UUID uuid, String key, String icon, String color, long seconds) {
        timers.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>())
                .put(key, new ActionbarTimer(icon, color, System.currentTimeMillis() + seconds * 1000L));
    }

    public void clear(UUID uuid) {
        timers.remove(uuid);
    }

    private static final class ActionbarTimer {
        final String icon;
        final String color;
        final long createdAtMs;
        final long endsAtMs;

        ActionbarTimer(String icon, String color, long endsAtMs) {
            this.icon = icon;
            this.color = color;
            this.createdAtMs = System.currentTimeMillis();
            this.endsAtMs = endsAtMs;
        }
    }
}