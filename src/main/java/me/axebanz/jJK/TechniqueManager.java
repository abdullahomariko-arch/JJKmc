package me.axebanz.jJK;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

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

    public Technique getAssigned(UUID uuid) {
        PlayerProfile prof = store.get(uuid);
        if (prof == null) return null;
        return registry.get(prof.techniqueId);
    }

    public String getAssignedId(UUID uuid) {
        PlayerProfile prof = store.get(uuid);
        return prof == null ? null : prof.techniqueId;
    }

    public void setTechnique(UUID uuid, String id) {
        PlayerProfile prof = store.get(uuid);
        if (prof == null) return;

        // Enforce exclusivity: if a technique is already set and it's different, block
        if (prof.techniqueId != null && !prof.techniqueId.isEmpty() && !prof.techniqueId.equalsIgnoreCase(id)) {
            Player p = getOnline(uuid);
            if (p != null) p.sendMessage("§cYou already have a cursed technique equipped.");
            return;
        }

        prof.techniqueId = id;
        store.save(uuid);
    }

    public void forceSetTechnique(UUID uuid, String id) {
        PlayerProfile prof = store.get(uuid);
        if (prof == null) return;
        prof.techniqueId = id;
        store.save(uuid);
    }

    /**
     * Central check for "technique actions" (including commands like /creation).
     * ISOH nullification should block these.
     */
    public boolean canUseTechniqueActions(Player p, boolean sendMessage) {
        UUID uuid = p.getUniqueId();
        if (!plugin.nullify().isNullified(uuid)) return true;

        if (sendMessage) {
            String prefix = plugin.cfg().prefix();
            long rem = plugin.nullify().remainingSeconds(uuid);
            p.sendMessage(prefix + "§cYou can't use your technique right now. §7(NULLIFIED: " + TimeFmt.mmss(rem) + ")");
        }
        return false;
    }

    /**
     * Can the player cast technique abilities (/jjk technique 1|2|3)?
     * Rules now:
     * - Must NOT be nullified
     * - Must HAVE a technique assigned
     */
    public boolean canUseTechnique(Player p) {
        UUID uuid = p.getUniqueId();
        if (plugin.nullify().isNullified(uuid)) return false;

        Technique t = getAssigned(uuid);
        return t != null && t.canUse(p);
    }

    public void cast(Player player, AbilitySlot slot) {
        if (slot == null) return;
        if (!canUseTechnique(player)) return;

        Technique t = getAssigned(player.getUniqueId());
        if (t == null) return;

        t.castAbility(player, slot);
    }

    public String techniqueColorHex(UUID uuid) {
        Technique t = getAssigned(uuid);
        return t == null ? "#AAAAAA" : t.hexColor();
    }

    public String techniqueName(UUID uuid) {
        Technique t = getAssigned(uuid);
        return t == null ? "None" : t.displayName();
    }

    public void notifyTechniqueState(Player p) {
        String prefix = plugin.cfg().prefix();
        p.sendMessage(prefix + "§7Technique: §f" + techniqueName(p.getUniqueId()));
    }

    public Player getOnline(UUID uuid) {
        return Bukkit.getPlayer(uuid);
    }
}