package me.axebanz.jJK;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

/**
 * Manages the Six Eyes trait for players.
 * Six Eyes holders get 200 max CE level and near-zero Limitless CE costs.
 * Stored persistently in the player's PersistentDataContainer (survives restarts).
 */
public final class SixEyesTrait {

    private final NamespacedKey key;

    public SixEyesTrait(JJKCursedToolsPlugin plugin) {
        this.key = new NamespacedKey(plugin, "six_eyes");
    }

    /** Returns true if the player has the Six Eyes trait. */
    public boolean hasSixEyes(Player p) {
        return p.getPersistentDataContainer().has(key, PersistentDataType.BYTE)
                && p.getPersistentDataContainer().get(key, PersistentDataType.BYTE) == (byte) 1;
    }

    /** Grants the Six Eyes trait to the player. */
    public void give(Player p) {
        p.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
    }

    /** Removes the Six Eyes trait from the player. */
    public void remove(Player p) {
        p.getPersistentDataContainer().remove(key);
    }

    /**
     * Returns the CE cost for Limitless abilities.
     * Six Eyes holders pay near-zero (1 CE, or 0 for most abilities).
     */
    public int scaleCost(Player p, int normalCost) {
        if (hasSixEyes(p)) return 0;
        return normalCost;
    }
}
