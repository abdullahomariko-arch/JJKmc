package me.axebanz.jJK;

import org.bukkit.entity.Chicken;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class KoganeEntity {

    private final JJKCursedToolsPlugin plugin;
    private final Map<UUID, Chicken> koganeMap = new HashMap<>();

    public KoganeEntity(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    public void spawnKogane(Player p) {
        despawnKogane(p);
        Chicken chicken = p.getWorld().spawn(p.getLocation().add(1, 0, 0), Chicken.class, c -> {
            c.setCustomName("§e§lKogane");
            c.setCustomNameVisible(true);
            c.setInvisible(true);
            c.setAI(false);
            c.setSilent(true);
            c.setAdult();
        });
        koganeMap.put(p.getUniqueId(), chicken);
    }

    public void despawnKogane(Player p) {
        Chicken existing = koganeMap.remove(p.getUniqueId());
        if (existing != null && !existing.isDead()) existing.remove();
    }

    public void sendInfo(Player p, String message) {
        p.sendActionBar("§e§lKogane: §r" + message);
    }
}
