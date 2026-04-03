package me.axebanz.jJK;

import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.UUID;

public final class TechniqueManager {

    private final JJKCursedToolsPlugin plugin;
    private final TechniqueRegistry registry;
    private final PlayerDataStore store;

    public TechniqueManager(JJKCursedToolsPlugin plugin, TechniqueRegistry registry, PlayerDataStore store) {
        this.plugin = plugin;
        this.registry = registry;
        this.store = store;
    }

    /** Returns the {@link Technique} assigned to the player, or {@code null} if none. */
    public Technique getAssigned(UUID uuid) {
        String id = store.get(uuid).techniqueId;
        if (id == null) return null;
        return registry.get(id);
    }

    /** Returns the technique ID string assigned to the player, or {@code null} if none. */
    public String getAssignedId(UUID uuid) {
        return store.get(uuid).techniqueId;
    }

    /** Assigns a technique to a player by ID and persists the change. */
    public void assign(UUID uuid, String techId) {
        PlayerProfile prof = store.get(uuid);
        prof.techniqueId = techId == null ? null : techId.toLowerCase(Locale.ROOT);
        store.save(uuid);
    }

    /** Admin force-set: assigns a technique and notifies the target player if online. */
    public void forceSetTechnique(UUID uuid, String techId) {
        assign(uuid, techId);
        Player p = plugin.getServer().getPlayer(uuid);
        if (p != null) {
            p.sendMessage(plugin.cfg().prefix() + "§aYour technique has been set to: §f" + techId);
        }
    }

    /**
     * Returns {@code true} if the player can currently use technique actions.
     * A player cannot use technique actions while nullified.
     *
     * @param p              the online player
     * @param notifyOnFail   if {@code true}, send a failure message to the player
     */
    public boolean canUseTechniqueActions(Player p, boolean notifyOnFail) {
        if (p == null || !p.isOnline()) return false;
        if (plugin.nullify().isNullified(p.getUniqueId())) {
            if (notifyOnFail) {
                p.sendMessage(plugin.cfg().prefix() + "§cYour technique is nullified!");
            }
            return false;
        }
        return true;
    }

    /** Casts a technique ability for the player at the given {@link AbilitySlot}. */
    public void cast(Player p, AbilitySlot slot) {
        Technique tech = getAssigned(p.getUniqueId());
        if (tech == null) {
            p.sendMessage(plugin.cfg().prefix() + "§cYou don't have a technique assigned.");
            return;
        }
        tech.castAbility(p, slot);
    }

    /** Returns the technique hex color for a player, or a default grey if no technique is assigned. */
    public String techniqueColorHex(UUID uuid) {
        Technique t = getAssigned(uuid);
        return t != null ? t.hexColor() : "#AAAAAA";
    }

    /** Returns the technique display name for a player, or {@code "None"} if no technique is assigned. */
    public String techniqueName(UUID uuid) {
        Technique t = getAssigned(uuid);
        return t != null ? t.displayName() : "None";
    }
}

