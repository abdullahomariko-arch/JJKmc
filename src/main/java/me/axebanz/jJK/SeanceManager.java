package me.axebanz.jJK;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages séance/reincarnation mechanics.
 * Bug Fix #7: findNearestArmorStandWithBody detects dropped Item entity near armor stand.
 * startIncantation stores dropped item entity UUID.
 * completeReincarnation removes the dropped item entity.
 */
public class SeanceManager {
    private final JJKCursedToolsPlugin plugin;
    private final Map<UUID, IncantationState> active = new HashMap<>();

    public SeanceManager(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Bug Fix #7: Instead of checking armor stand hand, find a dropped Item entity
     * (cursed body) within 2 blocks of any armor stand, and return that armor stand.
     */
    public ArmorStand findNearestArmorStandWithBody(Location near, double searchRadius) {
        if (near.getWorld() == null) return null;
        Collection<Entity> entities = near.getWorld().getNearbyEntities(near, searchRadius, searchRadius, searchRadius);

        for (Entity entity : entities) {
            if (!(entity instanceof ArmorStand stand)) continue;
            // Find a dropped cursed body item within 2 blocks of this armor stand
            Collection<Entity> nearby = stand.getWorld().getNearbyEntities(
                    stand.getLocation(), 2.0, 2.0, 2.0);
            for (Entity nearby2 : nearby) {
                if (!(nearby2 instanceof Item droppedItem)) continue;
                ItemStack stack = droppedItem.getItemStack();
                if (CursedBodyItem.isCursedBody(stack)) {
                    return stand;
                }
            }
        }
        return null;
    }

    /**
     * Bug Fix #7: Get the dropped item entity near the armor stand and store its UUID.
     */
    private Item findDroppedBodyNear(ArmorStand stand) {
        Collection<Entity> nearby = stand.getWorld().getNearbyEntities(
                stand.getLocation(), 2.0, 2.0, 2.0);
        for (Entity e : nearby) {
            if (!(e instanceof Item item)) continue;
            if (CursedBodyItem.isCursedBody(item.getItemStack())) {
                return item;
            }
        }
        return null;
    }

    public void startIncantation(Player incantator, ArmorStand stand) {
        UUID uuid = incantator.getUniqueId();
        if (active.containsKey(uuid)) {
            incantator.sendMessage(plugin.cfg().prefix() + "§cAlready performing séance.");
            return;
        }

        int duration = plugin.cfg().seanceIncantationTime();
        IncantationState state = new IncantationState(uuid, stand.getUniqueId(), duration);

        // Bug Fix #7: find and store dropped item entity UUID
        Item droppedItem = findDroppedBodyNear(stand);
        if (droppedItem != null) {
            state.setDroppedItemId(droppedItem.getUniqueId());
        }

        active.put(uuid, state);
        incantator.sendMessage(plugin.cfg().prefix() + "§5Séance begun...");
    }

    public void tickIncantations() {
        active.values().removeIf(state -> {
            state.tick();
            if (state.getTicksRemaining() <= 0 && !state.isComplete()) {
                state.setComplete(true);
                Player incantator = Bukkit.getPlayer(state.getIncantatorUuid());
                if (incantator != null) {
                    completeReincarnation(incantator, state);
                }
                return true;
            }
            return state.isComplete();
        });
    }

    /**
     * Bug Fix #7: Remove the dropped item entity on completion.
     */
    private void completeReincarnation(Player incantator, IncantationState state) {
        // Remove the dropped cursed body item entity from the world
        UUID droppedItemId = state.getDroppedItemId();
        if (droppedItemId != null) {
            Entity item = Bukkit.getEntity(droppedItemId);
            if (item instanceof Item) {
                item.remove();
            }
        }

        incantator.sendMessage(plugin.cfg().prefix() + "§5§lReincarnation complete!");
        // Additional reincarnation logic handled by SeanceListener
    }

    public void cancelIncantation(UUID uuid) {
        active.remove(uuid);
    }

    public IncantationState getState(UUID uuid) {
        return active.get(uuid);
    }

    public boolean isIncanting(UUID uuid) {
        return active.containsKey(uuid);
    }
}
