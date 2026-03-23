package me.axebanz.jJK;

import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class CullingGamesManager {

    private final JJKCursedToolsPlugin plugin;
    private File dataFile;
    private YamlConfiguration config;

    // Colony centers: "A", "B", "C", "D" -> Location (x, y=64, z, world)
    private final Map<String, Location> colonyCenters = new HashMap<>();
    // Player UUID -> colony ID
    private final Map<UUID, String> playerColony = new HashMap<>();
    // Player UUID -> points
    private final Map<UUID, Integer> playerPoints = new HashMap<>();

    public static final int COLONY_HALF_SIZE = 500;
    public static final List<String> COLONY_IDS = List.of("A", "B", "C", "D");

    public CullingGamesManager(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
        dataFile = new File(plugin.getDataFolder(), "cullinggames.yml");
        loadData();
    }

    public void registerPlayer(Player p) {
        UUID uuid = p.getUniqueId();
        // If already registered, just confirm
        if (playerColony.containsKey(uuid)) {
            p.sendMessage(plugin.cfg().prefix() + "§eYou are already in Colony §6" + playerColony.get(uuid));
            return;
        }
        // Assign to colony with fewest members
        String assigned = assignColony(uuid);
        playerColony.put(uuid, assigned);
        playerPoints.put(uuid, 0);

        p.sendMessage(plugin.cfg().prefix() + "§eWelcome to Colony §6" + assigned + "§e!");
        teleportToColony(p, assigned);
        saveData();
    }

    private String assignColony(UUID uuid) {
        // Count members per colony
        Map<String, Integer> counts = new HashMap<>();
        for (String id : COLONY_IDS) counts.put(id, 0);
        for (String col : playerColony.values()) counts.merge(col, 1, Integer::sum);

        // Pick the one with fewest members
        return counts.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("A");
    }

    public void teleportToColony(Player p, String colonyId) {
        String id = colonyId.toUpperCase();
        if (!COLONY_IDS.contains(id)) {
            p.sendMessage(plugin.cfg().prefix() + "§cInvalid colony. Choose A, B, C, or D.");
            return;
        }
        Location center = colonyCenters.get(id);
        if (center == null) {
            p.sendMessage(plugin.cfg().prefix() + "§cColony " + id + " has no spawn set. Use /cullinggames setup.");
            return;
        }
        // Random location inside colony
        Random rng = new Random();
        int ox = rng.nextInt(COLONY_HALF_SIZE * 2) - COLONY_HALF_SIZE;
        int oz = rng.nextInt(COLONY_HALF_SIZE * 2) - COLONY_HALF_SIZE;
        World w = center.getWorld();
        if (w == null) { p.sendMessage(plugin.cfg().prefix() + "§cColony world not found."); return; }
        Location dest = w.getHighestBlockAt(center.getBlockX() + ox, center.getBlockZ() + oz).getLocation().add(0, 1, 0);
        p.teleport(dest);
        p.sendMessage(plugin.cfg().prefix() + "§eTeleported to Colony §6" + id);
    }

    public String getColony(Player p) {
        return playerColony.get(p.getUniqueId());
    }

    public void addPoints(UUID uuid, int amount) {
        playerPoints.merge(uuid, amount, Integer::sum);
        saveData();
    }

    public int getPoints(UUID uuid) {
        return playerPoints.getOrDefault(uuid, 0);
    }

    public void transferPoints(Player from, Player to, int amount) {
        UUID fromUUID = from.getUniqueId();
        UUID toUUID = to.getUniqueId();
        int current = getPoints(fromUUID);
        if (current < amount) {
            from.sendMessage(plugin.cfg().prefix() + "§cNot enough points. You have §f" + current);
            return;
        }
        playerPoints.merge(fromUUID, -amount, Integer::sum);
        playerPoints.merge(toUUID, amount, Integer::sum);
        from.sendMessage(plugin.cfg().prefix() + "§eTransferred §6" + amount + "§e points to §6" + to.getName());
        to.sendMessage(plugin.cfg().prefix() + "§eReceived §6" + amount + "§e points from §6" + from.getName());
        saveData();
    }

    public void addRule(Player p, String rule) {
        UUID uuid = p.getUniqueId();
        int pts = getPoints(uuid);
        if (pts < 100) {
            p.sendMessage(plugin.cfg().prefix() + "§cAdding a rule costs §6100 points§c. You have §f" + pts);
            return;
        }
        playerPoints.merge(uuid, -100, Integer::sum);
        List<String> rules = config.getStringList("rules");
        rules.add(p.getName() + ": " + rule);
        config.set("rules", rules);
        saveConfig();

        Bukkit.broadcastMessage(plugin.cfg().prefix() + "§6§lNEW CULLING GAMES RULE §eby §6" + p.getName() + "§e: §f" + rule);
    }

    public void setColonyCenter(String colonyId, int x, int z, World world) {
        String id = colonyId.toUpperCase();
        Location loc = new Location(world, x, 64, z);
        colonyCenters.put(id, loc);
        config.set("colonies." + id + ".world", world.getName());
        config.set("colonies." + id + ".x", x);
        config.set("colonies." + id + ".z", z);
        saveConfig();
    }

    public String getColonyInfo(Player p) {
        String colony = playerColony.get(p.getUniqueId());
        int pts = getPoints(p.getUniqueId());
        if (colony == null) return "§cNot registered in the Culling Games.";
        return "§eColony: §6" + colony + " §e| Points: §6" + pts;
    }

    public boolean isNearBarrier(Player p) {
        String colonyId = playerColony.get(p.getUniqueId());
        if (colonyId == null) return false;
        Location center = colonyCenters.get(colonyId);
        if (center == null) return false;
        Location loc = p.getLocation();
        if (!loc.getWorld().equals(center.getWorld())) return false;

        int dx = Math.abs(loc.getBlockX() - center.getBlockX());
        int dz = Math.abs(loc.getBlockZ() - center.getBlockZ());
        int edge = COLONY_HALF_SIZE - 2;
        return dx >= edge || dz >= edge;
    }

    public boolean isInsideColony(Player p, String colonyId) {
        Location center = colonyCenters.get(colonyId.toUpperCase());
        if (center == null) return false;
        Location loc = p.getLocation();
        if (!loc.getWorld().equals(center.getWorld())) return false;
        return Math.abs(loc.getBlockX() - center.getBlockX()) <= COLONY_HALF_SIZE
                && Math.abs(loc.getBlockZ() - center.getBlockZ()) <= COLONY_HALF_SIZE;
    }

    public void saveData() {
        if (config == null) return;
        for (Map.Entry<UUID, String> entry : playerColony.entrySet()) {
            String path = "players." + entry.getKey();
            config.set(path + ".colony", entry.getValue());
            config.set(path + ".points", playerPoints.getOrDefault(entry.getKey(), 0));
        }
        saveConfig();
    }

    public void loadData() {
        if (!dataFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Could not create cullinggames.yml: " + e.getMessage());
            }
        }
        config = YamlConfiguration.loadConfiguration(dataFile);

        // Load colony centers
        if (config.isConfigurationSection("colonies")) {
            for (String id : COLONY_IDS) {
                String worldName = config.getString("colonies." + id + ".world");
                if (worldName == null) continue;
                World w = Bukkit.getWorld(worldName);
                if (w == null) continue;
                int x = config.getInt("colonies." + id + ".x", 0);
                int z = config.getInt("colonies." + id + ".z", 0);
                colonyCenters.put(id, new Location(w, x, 64, z));
            }
        }

        // Load player data
        if (config.isConfigurationSection("players")) {
            for (String uuidStr : config.getConfigurationSection("players").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    String colony = config.getString("players." + uuidStr + ".colony");
                    int pts = config.getInt("players." + uuidStr + ".points", 0);
                    if (colony != null) playerColony.put(uuid, colony);
                    playerPoints.put(uuid, pts);
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    private void saveConfig() {
        try { config.save(dataFile); } catch (IOException e) {
            plugin.getLogger().warning("Could not save cullinggames.yml: " + e.getMessage());
        }
    }
}
