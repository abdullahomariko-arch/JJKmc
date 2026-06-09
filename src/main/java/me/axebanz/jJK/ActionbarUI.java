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
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
        }

        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!plugin.cfg().cooldownActionbarEnabled()) return;

            long now = System.currentTimeMillis();

            for (Player p : Bukkit.getOnlinePlayers()) {
                UUID uuid = p.getUniqueId();
                Map<String, ActionbarTimer> map = timers.get(uuid);

                String techId = plugin.techniqueManager().getAssignedId(uuid);

                // Energy Discharge special HUD:
                // [heat bar]     [charge bar]
                if ("energy_discharge".equalsIgnoreCase(techId)) {
                    sendEnergyDischargeHud(p);
                    continue;
                }

                String iconPrefix = buildTechniqueIcon(uuid);

                if (map == null || map.isEmpty()) {
                    sendMiniActionBar(p, iconPrefix);
                    continue;
                }

                map.values().removeIf(t -> t.endsAtMs <= now);
                if (map.isEmpty()) {
                    sendMiniActionBar(p, iconPrefix);
                    continue;
                }

                // Cursed Speech special multi-part display
                if (map.containsKey("cursed_speech.plummet")
                        || map.containsKey("cursed_speech.nomove")
                        || map.containsKey("cursed_speech.explode")) {

                    String msg = renderCursedSpeechBar(map);
                    if (msg != null && !msg.isBlank()) {
                        sendMiniActionBar(p, iconPrefix + "  <dark_gray>|</dark_gray>  " + msg);
                        continue;
                    }
                }

                // Default single timer display
                ActionbarTimer chosen = choose(map.values(), plugin.cfg().cooldownPreferShortest(), now);
                long remSec = Math.max(0, (chosen.endsAtMs - now) / 1000L);
                String msg = chosen.color + chosen.icon + " <white>" + TimeFmt.mmss(remSec) + "</white>";
                sendMiniActionBar(p, iconPrefix + "  <dark_gray>|</dark_gray>  " + msg);
            }
        }, 20L, 2L);
    }

    private void sendMiniActionBar(Player p, String miniMessageString) {
        try {
            Component component = MINI.deserialize(miniMessageString);
            p.sendActionBar(component);
        } catch (Exception e) {
            p.sendActionBar(Component.text(miniMessageString));
        }
    }

    private String buildTechniqueIcon(UUID uuid) {
        Technique tech = plugin.techniqueManager().getAssigned(uuid);

        if (tech == null) {
            return NO_TECHNIQUE_GLYPH + "<shift:4><dark_gray>No Technique</dark_gray>";
        }

        String glyph = tech.glyphTag();
        String name = tech.displayName();
        String cleanName = stripLegacyColors(name);

        if (plugin.nullify().isNullified(uuid)) {
            long rem = plugin.nullify().remainingSeconds(uuid);
            return "<red>✖ <strikethrough>NULLIFIED</strikethrough></red> <dark_gray>(<white>"
                    + TimeFmt.mmss(rem) + "</white>)</dark_gray>";
        }

        String hexColor = tech.hexColor();
        return glyph + "<shift:4><" + hexColor + ">" + cleanName + "</" + hexColor + ">";
    }

    private String stripLegacyColors(String input) {
        if (input == null) return "";
        return input.replaceAll("§[0-9a-fk-or]", "");
    }

    private String renderCursedSpeechBar(Map<String, ActionbarTimer> map) {
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
        if (t == null) {
            return "<gray>{" + label + "<gray>: </gray><green>Ready</green><gray>}</gray>";
        }

        long remSec = Math.max(0, (t.endsAtMs - System.currentTimeMillis()) / 1000L);
        return "<gray>{" + label + "<gray>: </gray><white>" + TimeFmt.mmss(remSec) + "</white><gray>}</gray>";
    }

    private ActionbarTimer choose(Collection<ActionbarTimer> values, boolean preferShortest, long now) {
        ActionbarTimer best = null;

        for (ActionbarTimer t : values) {
            if (best == null) {
                best = t;
                continue;
            }

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

    /**
     * Energy Discharge HUD only:
     * [heat bar]     [charge bar]
     *
     * No technique name, no black separator text, no merged font key.
     */
    private void sendEnergyDischargeHud(Player p) {
        EnergyDischargeManager ed = plugin.energyDischarge();

        int chargePercent = 0;
        int heatPercent = 0;

        if (ed != null) {
            GraniteBlastSession session = ed.getBlastSession(p.getUniqueId());
            if (session != null && session.isCharging()) {
                chargePercent = session.getChargePercent();
            }

            heatPercent = ed.getHeatPercent(p.getUniqueId());
        }

        Component heatBar = HeatBar.getBarComponent(heatPercent);
        Component spacer = Component.text("     ");
        Component chargeBar = GraniteChargeBar.getBarComponent(chargePercent);

        p.sendActionBar(heatBar.append(spacer).append(chargeBar));
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