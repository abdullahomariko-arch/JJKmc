package me.axebanz.jJK;

import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class ContractManager {

    private final JJKCursedToolsPlugin plugin;
    private File dataFile;
    private YamlConfiguration config;

    // player name (lowercase) -> assigned technique id
    private final Map<String, String> contractTechniques = new HashMap<>();
    // creator UUID -> list of target player names
    private final Map<UUID, List<String>> creatorContracts = new HashMap<>();

    public static final int MAX_CONTRACTS = 3;
    public static final List<String> CONTRACT_TECHNIQUES = List.of(
            "granite_blast", "thin_icebreaker", "contractual_contracts"
    );

    public ContractManager(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
        dataFile = new File(plugin.getDataFolder(), "contracts.yml");
        loadData();
    }

    /**
     * Create a contract for a new player, assigning them a technique.
     * Only works for players with the curse_manipulation technique.
     */
    public boolean createNewPlayerContract(Player creator, String targetPlayerName, String techniqueId) {
        UUID creatorUUID = creator.getUniqueId();
        String lowerName = targetPlayerName.toLowerCase();

        // Check creator has curse manipulation
        String creatorTech = plugin.techniqueManager().getAssignedId(creatorUUID);
        if (!"curse_manipulation".equalsIgnoreCase(creatorTech)) {
            creator.sendMessage(plugin.cfg().prefix() + "§cOnly Curse Manipulation users can create contracts.");
            return false;
        }

        // Validate technique
        if (!CONTRACT_TECHNIQUES.contains(techniqueId.toLowerCase())) {
            creator.sendMessage(plugin.cfg().prefix() + "§cInvalid technique. Choose: " + String.join(", ", CONTRACT_TECHNIQUES));
            return false;
        }

        // Check max contracts
        List<String> existing = creatorContracts.computeIfAbsent(creatorUUID, k -> new ArrayList<>());
        if (existing.size() >= MAX_CONTRACTS) {
            creator.sendMessage(plugin.cfg().prefix() + "§cYou already have the maximum of " + MAX_CONTRACTS + " contracts.");
            return false;
        }

        // Check if player already has a contract
        if (contractTechniques.containsKey(lowerName)) {
            creator.sendMessage(plugin.cfg().prefix() + "§c" + targetPlayerName + " already has a contract.");
            return false;
        }

        contractTechniques.put(lowerName, techniqueId.toLowerCase());
        existing.add(lowerName);
        saveData();

        creator.sendMessage(plugin.cfg().prefix() + "§5Contract created for §d" + targetPlayerName + "§5 with technique: §d" + techniqueId);
        return true;
    }

    public String getContractTechnique(String playerName) {
        return contractTechniques.get(playerName.toLowerCase());
    }

    public void handleNewPlayerJoin(Player newPlayer) {
        String lowerName = newPlayer.getName().toLowerCase();
        String technique = contractTechniques.get(lowerName);
        if (technique == null) return;

        // Assign technique
        plugin.techniqueManager().assign(newPlayer.getUniqueId(), technique);
        newPlayer.sendMessage(plugin.cfg().prefix() + "§5A contract has been fulfilled. You have been assigned: §d" + technique);

        // Teleport to a culling games colony if available
        if (plugin.cullingGames() != null) {
            plugin.cullingGames().registerPlayer(newPlayer);
        }
    }

    public void giveContractItem(Player p) {
        p.getInventory().addItem(CursedSealItem.create());
        p.sendMessage(plugin.cfg().prefix() + "§4Cursed Seal given.");
    }

    public void reincarnatePlayer(Player target, Player contractUser) {
        if (plugin.cullingGames() != null) {
            // Teleport to a random colony
            List<String> colonies = CullingGamesManager.COLONY_IDS;
            String colony = colonies.get(new Random().nextInt(colonies.size()));
            plugin.cullingGames().teleportToColony(target, colony);
            contractUser.sendMessage(plugin.cfg().prefix() + "§5Reincarnated §d" + target.getName() + "§5 to Colony §d" + colony);
        }
    }

    public void saveData() {
        for (Map.Entry<String, String> entry : contractTechniques.entrySet()) {
            config.set("contracts." + entry.getKey() + ".technique", entry.getValue());
        }
        // Save creator -> targets
        for (Map.Entry<UUID, List<String>> entry : creatorContracts.entrySet()) {
            config.set("creators." + entry.getKey() + ".targets", entry.getValue());
        }
        try { config.save(dataFile); } catch (IOException e) {
            plugin.getLogger().warning("Could not save contracts.yml: " + e.getMessage());
        }
    }

    public void loadData() {
        if (!dataFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Could not create contracts.yml: " + e.getMessage());
            }
        }
        config = YamlConfiguration.loadConfiguration(dataFile);

        if (config.isConfigurationSection("contracts")) {
            for (String name : config.getConfigurationSection("contracts").getKeys(false)) {
                String tech = config.getString("contracts." + name + ".technique");
                if (tech != null) contractTechniques.put(name, tech);
            }
        }
        if (config.isConfigurationSection("creators")) {
            for (String uuidStr : config.getConfigurationSection("creators").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    List<String> targets = config.getStringList("creators." + uuidStr + ".targets");
                    creatorContracts.put(uuid, new ArrayList<>(targets));
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }
}
