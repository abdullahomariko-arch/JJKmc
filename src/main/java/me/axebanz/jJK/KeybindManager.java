package me.axebanz.jJK;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages ability keybinds per player.
 * Keybinds are stored in the player's PersistentDataContainer for persistence.
 *
 * Supported keys: F, Q, SHIFT, RIGHT_CLICK, LEFT_CLICK
 * Example: /jjk keybind blue F  → pressing F fires Blue
 */
public final class KeybindManager {

    /** Supported key names. */
    public static final List<String> SUPPORTED_KEYS = List.of("F", "Q", "SHIFT", "RIGHT_CLICK", "LEFT_CLICK");

    private final JJKCursedToolsPlugin plugin;
    private final NamespacedKey pdcKey;

    /** In-memory cache: player UUID → (key → abilityId) */
    private final Map<UUID, Map<String, String>> keybinds = new ConcurrentHashMap<>();

    /** UI-Switching: tracks when a player started "holding" a press for an ability key */
    private final Map<UUID, Map<String, Long>> holdStart = new ConcurrentHashMap<>();

    public KeybindManager(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
        this.pdcKey = new NamespacedKey(plugin, "jjk_keybinds");
    }

    // ───────────────────────── Bind / Query ─────────────────────────

    /**
     * Binds an ability to a key for the given player.
     * @param p       the player
     * @param ability ability identifier (e.g. "blue", "red", "infinity")
     * @param key     key identifier (F, Q, SHIFT, RIGHT_CLICK, LEFT_CLICK)
     */
    public void bind(Player p, String ability, String key) {
        String normKey = key.toUpperCase(Locale.ROOT);
        Map<String, String> map = keybinds.computeIfAbsent(p.getUniqueId(), k -> new HashMap<>());
        map.put(normKey, ability.toLowerCase(Locale.ROOT));
        saveToPdc(p);
    }

    /**
     * Removes the keybind on a specific key for the player.
     */
    public void unbind(Player p, String key) {
        String normKey = key.toUpperCase(Locale.ROOT);
        Map<String, String> map = keybinds.get(p.getUniqueId());
        if (map != null) map.remove(normKey);
        saveToPdc(p);
    }

    /**
     * Returns the ability bound to the given key, or null if none.
     */
    public String getAbilityForKey(Player p, String key) {
        Map<String, String> map = keybinds.get(p.getUniqueId());
        if (map == null) return null;
        return map.get(key.toUpperCase(Locale.ROOT));
    }

    /**
     * Returns a copy of all keybinds for a player (key → ability).
     */
    public Map<String, String> getKeybinds(Player p) {
        Map<String, String> map = keybinds.get(p.getUniqueId());
        if (map == null) return Map.of();
        return Map.copyOf(map);
    }

    // ───────────────────────── UI Switching (Hold Detection) ─────────────────────────

    /**
     * Called when a player begins holding a key (first press/activation).
     * Returns the ability bound to that key, or null.
     */
    public String onKeyPress(Player p, String key) {
        String ability = getAbilityForKey(p, key);
        if (ability == null) return null;
        holdStart
            .computeIfAbsent(p.getUniqueId(), k -> new HashMap<>())
            .put(key.toUpperCase(Locale.ROOT), System.currentTimeMillis());
        return ability;
    }

    /**
     * Called when a player releases (or the event resolves immediately).
     * Returns the ability and whether it should be Maximum Output (held 2+ seconds).
     * Returns null if no keybind was active.
     */
    public KeypressResult onKeyRelease(Player p, String key) {
        String normKey = key.toUpperCase(Locale.ROOT);
        String ability = getAbilityForKey(p, normKey);
        if (ability == null) return null;

        Map<String, Long> holdMap = holdStart.get(p.getUniqueId());
        if (holdMap == null) return new KeypressResult(ability, false);

        Long start = holdMap.remove(normKey);
        if (start == null) return new KeypressResult(ability, false);

        long elapsed = System.currentTimeMillis() - start;
        boolean maxOutput = elapsed >= 2000L;
        return new KeypressResult(ability, maxOutput);
    }

    /** Checks if a key is currently being "held" (press registered but not yet released). */
    public boolean isHolding(Player p, String key) {
        Map<String, Long> holdMap = holdStart.get(p.getUniqueId());
        if (holdMap == null) return false;
        return holdMap.containsKey(key.toUpperCase(Locale.ROOT));
    }

    /** Returns elapsed hold time in milliseconds for a key, or -1 if not held. */
    public long holdElapsedMs(Player p, String key) {
        Map<String, Long> holdMap = holdStart.get(p.getUniqueId());
        if (holdMap == null) return -1L;
        Long start = holdMap.get(key.toUpperCase(Locale.ROOT));
        if (start == null) return -1L;
        return System.currentTimeMillis() - start;
    }

    // ───────────────────────── Persistence ─────────────────────────

    /** Loads keybinds from the player's PersistentDataContainer into memory. */
    public void loadFromPdc(Player p) {
        PersistentDataContainer pdc = p.getPersistentDataContainer();
        if (!pdc.has(pdcKey, PersistentDataType.STRING)) return;
        String raw = pdc.get(pdcKey, PersistentDataType.STRING);
        if (raw == null || raw.isBlank()) return;

        Map<String, String> map = new HashMap<>();
        for (String entry : raw.split(";")) {
            String[] parts = entry.split("=", 2);
            if (parts.length == 2 && !parts[0].isBlank() && !parts[1].isBlank()) {
                map.put(parts[0], parts[1]);
            }
        }
        keybinds.put(p.getUniqueId(), map);
    }

    /** Saves keybinds from memory to the player's PersistentDataContainer. */
    public void saveToPdc(Player p) {
        Map<String, String> map = keybinds.get(p.getUniqueId());
        PersistentDataContainer pdc = p.getPersistentDataContainer();
        if (map == null || map.isEmpty()) {
            pdc.remove(pdcKey);
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : map.entrySet()) {
            if (sb.length() > 0) sb.append(';');
            sb.append(e.getKey()).append('=').append(e.getValue());
        }
        pdc.set(pdcKey, PersistentDataType.STRING, sb.toString());
    }

    /** Clears in-memory data for a player (called on quit). */
    public void onQuit(UUID uuid) {
        keybinds.remove(uuid);
        holdStart.remove(uuid);
    }

    // ───────────────────────── Inner Types ─────────────────────────

    /** Result of onKeyRelease: which ability and whether maximum output should fire. */
    public static final class KeypressResult {
        public final String ability;
        public final boolean maxOutput;

        public KeypressResult(String ability, boolean maxOutput) {
            this.ability = ability;
            this.maxOutput = maxOutput;
        }
    }
}
