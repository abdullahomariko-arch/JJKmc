package me.axebanz.jJK;

import org.bukkit.entity.Entity;

/**
 * Bridge to BetterModel API.
 * Provides safe wrappers that no-op if BetterModel is not present.
 */
public class BetterModelBridge {
    private static boolean available = false;

    static {
        try {
            Class.forName("kr.toxicity.model.api.BetterModelAPI");
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
        // BetterModel integration would go here
    }

    public static void removeModel(Entity entity) {
        if (!available) return;
        // BetterModel integration would go here
    }
}
