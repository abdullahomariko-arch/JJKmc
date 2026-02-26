package me.axebanz.jJK;

import org.bukkit.entity.Entity;

/**
 * Bridge to ModelEngine API.
 * Provides safe wrappers that no-op if ModelEngine is not present.
 */
public class ModelEngineBridge {
    private static boolean available = false;

    static {
        try {
            Class.forName("com.ticxo.modelengine.api.ModelEngineAPI");
            available = true;
        } catch (ClassNotFoundException ignored) {
            available = false;
        }
    }

    public static boolean isAvailable() {
        return available;
    }

    public static void spawnModel(Entity entity, String modelId) {
        if (!available) return;
        // ModelEngine integration would go here
    }

    public static void removeModel(Entity entity) {
        if (!available) return;
        // ModelEngine integration would go here
    }
}
