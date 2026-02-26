package me.axebanz.jJK;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Domain Expansion for Idle Death Gamble technique.
 * Bug Fix #11: Override getBarrierMaterial() to return WHITE_CONCRETE.
 */
public class IdleDeathGambleDomain extends DomainExpansion {

    public IdleDeathGambleDomain(JJKCursedToolsPlugin plugin, UUID ownerUuid, Location center) {
        super(plugin, ownerUuid, center);
    }

    @Override
    public String getId() { return "idle_death_gamble_domain"; }

    @Override
    public String getDisplayName() { return "§eIdle Death Gamble — Domain"; }

    // Bug Fix #11: WHITE_CONCRETE barrier
    @Override
    protected Material getBarrierMaterial() {
        return Material.WHITE_CONCRETE;
    }

    @Override
    protected void onOpen(Player player) {
        player.sendMessage(plugin.cfg().prefix() + "§eIdle Death Gamble §7domain opened!");
    }

    @Override
    protected void onClose(Player player) {
        player.sendMessage(plugin.cfg().prefix() + "§eIdle Death Gamble §7domain closed.");
    }
}
