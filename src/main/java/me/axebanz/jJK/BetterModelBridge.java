package me.axebanz.jJK;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.data.renderer.ModelRenderer;
import kr.toxicity.model.api.tracker.EntityTracker;
import kr.toxicity.model.api.tracker.DummyTracker;
import kr.toxicity.model.api.tracker.Tracker;
import kr.toxicity.model.api.bukkit.platform.BukkitAdapter;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BetterModelBridge {

    private final JJKCursedToolsPlugin plugin;
    private final Map<UUID, EntityTracker> entityTrackers = new ConcurrentHashMap<>();
    private final Map<Location, DummyTracker> locationTrackers = new ConcurrentHashMap<>();

    public BetterModelBridge(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Attach a model to a player (disguise).
     */
    public void disguisePlayer(Player player, String modelName) {
        try {
            Optional<ModelRenderer> renderer = BetterModel.model(modelName);
            if (renderer.isEmpty()) {
                plugin.getLogger().warning("[BetterModel] Model '" + modelName + "' not found!");
                return;
            }
            EntityTracker tracker = renderer.get().getOrCreate(BukkitAdapter.adapt(player));
            entityTrackers.put(player.getUniqueId(), tracker);
        } catch (Exception ex) {
            plugin.getLogger().warning("[BetterModel] disguisePlayer failed: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Spawn a model at a location (for non-player entities).
     */
    public void spawnModelAt(Location location, String modelName) {
        try {
            Optional<ModelRenderer> renderer = BetterModel.model(modelName);
            if (renderer.isEmpty()) {
                plugin.getLogger().warning("[BetterModel] Model '" + modelName + "' not found!");
                return;
            }
            DummyTracker tracker = renderer.get().create(BukkitAdapter.adapt(location));
            locationTrackers.put(location, tracker);
        } catch (Exception ex) {
            plugin.getLogger().warning("[BetterModel] spawnModelAt failed: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Remove model from a player (undisguise).
     */
    public void undisguisePlayer(Player player, String modelName) {
        try {
            EntityTracker tracker = entityTrackers.remove(player.getUniqueId());
            if (tracker != null) {
                tracker.close();
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("[BetterModel] undisguisePlayer failed: " + ex.getMessage());
        }
    }

    /**
     * Remove glass model near a location.
     */
    public void removeGlassAt(Location location, double radius) {
        try {
            DummyTracker tracker = locationTrackers.remove(location);
            if (tracker != null) {
                tracker.close();
                return;
            }
            locationTrackers.entrySet().removeIf(entry -> {
                if (entry.getKey().distanceSquared(location) <= radius * radius) {
                    entry.getValue().close();
                    return true;
                }
                return false;
            });
        } catch (Exception ex) {
            plugin.getLogger().warning("[BetterModel] removeGlassAt failed: " + ex.getMessage());
        }
    }

    /**
     * Play an animation on a model at a location.
     * Spawns the model, plays the animation, then removes it after 15 ticks.
     */
    public void testAnimation(String modelName, String animationName, Location location) {
        try {
            Optional<ModelRenderer> renderer = BetterModel.model(modelName);
            if (renderer.isEmpty()) {
                plugin.getLogger().warning("[BetterModel] Model '" + modelName + "' not found!");
                return;
            }
            DummyTracker tracker = renderer.get().create(BukkitAdapter.adapt(location));
            tracker.animate(animationName);
        } catch (Exception ex) {
            plugin.getLogger().warning("[BetterModel] testAnimation failed: " + ex.getMessage());
        }
    }

    /**
     * Attach the glass model to any entity.
     */
    public void attachGlass(Entity entity) {
        try {
            Optional<ModelRenderer> renderer = BetterModel.model("glass");
            if (renderer.isEmpty()) {
                plugin.getLogger().warning("[BetterModel] Model 'glass' not found!");
                return;
            }
            EntityTracker tracker = renderer.get().getOrCreate(BukkitAdapter.adapt(entity));
            entityTrackers.put(entity.getUniqueId(), tracker);
        } catch (Exception ex) {
            plugin.getLogger().warning("[BetterModel] attachGlass failed: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Remove glass model from an entity.
     */
    public void removeGlass(UUID entityId) {
        try {
            EntityTracker tracker = entityTrackers.remove(entityId);
            if (tracker != null) {
                tracker.close();
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("[BetterModel] removeGlass failed: " + ex.getMessage());
        }
    }
}