package me.axebanz.jJK;

import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class SeanceManager {

    private final JJKCursedToolsPlugin plugin;
    private final NamespacedKey KEY_BINDING_VOW;

    private static class IncantationState {
        UUID armorStandId;
        Location armorStandLocation;
        UUID deadPlayerUuid;
        String techniqueId;
        int ticksRemaining = 1200;
        int taskId = -1;
    }

    private static class SeanceState {
        UUID reincarnatedUuid;
        boolean bindingVowActive;
        int drainTaskId = -1;
    }

    private final Map<UUID, IncantationState> incantations = new HashMap<>();
    private final Map<UUID, SeanceState> seances = new HashMap<>();
    private final Map<UUID, UUID> reincarnatedByMap = new HashMap<>();

    private Location waitingRoomLocation = null;
    private final File waitingRoomFile;

    public SeanceManager(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
        this.KEY_BINDING_VOW = new NamespacedKey(plugin, "binding_vow");
        this.waitingRoomFile = new File(plugin.getDataFolder(), "waitingroom.yml");
        loadWaitingRoom();
    }

    private void loadWaitingRoom() {
        if (!waitingRoomFile.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(waitingRoomFile);
        String worldName = y.getString("world");
        if (worldName == null) return;
        World world = Bukkit.getWorld(worldName);
        if (world == null) return;
        double x = y.getDouble("x", 0);
        double y2 = y.getDouble("y", 64);
        double z = y.getDouble("z", 0);
        float yaw = (float) y.getDouble("yaw", 0);
        float pitch = (float) y.getDouble("pitch", 0);
        this.waitingRoomLocation = new Location(world, x, y2, z, yaw, pitch);
    }

    public void saveWaitingRoom(Location loc) {
        this.waitingRoomLocation = loc.clone();
        YamlConfiguration y = new YamlConfiguration();
        y.set("world", loc.getWorld().getName());
        y.set("x", loc.getX());
        y.set("y", loc.getY());
        y.set("z", loc.getZ());
        y.set("yaw", loc.getYaw());
        y.set("pitch", loc.getPitch());
        try {
            if (!waitingRoomFile.getParentFile().exists()) waitingRoomFile.getParentFile().mkdirs();
            y.save(waitingRoomFile);
        } catch (IOException ex) {
            plugin.getLogger().warning("Failed saving waiting room: " + ex.getMessage());
        }
    }

    public Location getWaitingRoom() {
        if (waitingRoomLocation != null) return waitingRoomLocation.clone();
        World world = Bukkit.getWorlds().get(0);
        return world.getSpawnLocation().clone().add(0.5, 0, 0.5);
    }

    public boolean isWaitingRoomSet() {
        return waitingRoomLocation != null;
    }

    public void banPermadeadPlayer(Player p) {
        p.banPlayer("§5§lYou have died.\n§7Your soul awaits reincarnation...\n§7A séance user must revive you.");
        if (p.isOnline()) {
            p.kickPlayer("§5§lYou have died.\n§7Your soul awaits reincarnation...\n§7A séance user must revive you.");
        }
    }

    public void unbanPlayer(UUID uuid) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
        if (op.isBanned()) {
            Bukkit.getBanList(BanList.Type.NAME).pardon(op.getName());
        }
        try {
            org.bukkit.BanList profileBanList = Bukkit.getBanList(BanList.Type.PROFILE);
            if (profileBanList.isBanned(op.getName())) {
                profileBanList.pardon(op.getName());
            }
        } catch (Exception ignored) {}
    }

    public void startIncantation(Player seanceUser) {
        UUID uuid = seanceUser.getUniqueId();
        String prefix = plugin.cfg().prefix();

        if (incantations.containsKey(uuid)) {
            seanceUser.sendMessage(prefix + "§cYou already have an incantation in progress.");
            return;
        }
        if (seances.containsKey(uuid)) {
            seanceUser.sendMessage(prefix + "§cYou already have an active séance.");
            return;
        }

        ArmorStand stand = findNearestArmorStand(seanceUser.getLocation(), 3.0);
        if (stand == null) {
            seanceUser.sendMessage(prefix + "§cNo armor stand found within 3 blocks.");
            return;
        }

        Item groundBody = findCursedBodyOnGround(stand.getLocation(), 2.0);
        if (groundBody == null) {
            seanceUser.sendMessage(prefix + "§cNo cursed body found on the ground near the armor stand.");
            return;
        }

        ItemStack bodyItem = groundBody.getItemStack();
        UUID deadPlayerUuid = plugin.cursedBody().source(bodyItem);
        if (deadPlayerUuid == null) {
            seanceUser.sendMessage(prefix + "§cThe cursed body on the ground is invalid.");
            return;
        }

        plugin.data().load(deadPlayerUuid);
        PlayerProfile deadProfile = plugin.data().get(deadPlayerUuid);
        if (!deadProfile.permaDead) {
            seanceUser.sendMessage(prefix + "§cThat sorcerer has not fallen yet.");
            return;
        }

        String techniqueId = plugin.cursedBody().getTechniqueId(bodyItem);

        groundBody.remove();

        IncantationState state = new IncantationState();
        state.armorStandId = stand.getUniqueId();
        state.armorStandLocation = stand.getLocation().clone();
        state.deadPlayerUuid = deadPlayerUuid;
        state.techniqueId = techniqueId;

        int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> tickIncantation(uuid), 1L, 1L);
        state.taskId = taskId;
        incantations.put(uuid, state);

        seanceUser.sendMessage(prefix + "§5§lThe séance begins... Focus your cursed energy.");
    }

    private Item findCursedBodyOnGround(Location center, double radius) {
        for (Entity e : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (!(e instanceof Item item)) continue;
            if (plugin.cursedBody().isCursedBody(item.getItemStack())) {
                return item;
            }
        }
        return null;
    }

    private ArmorStand findNearestArmorStand(Location center, double radius) {
        ArmorStand nearest = null;
        double bestDist = Double.MAX_VALUE;
        for (Entity e : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (!(e instanceof ArmorStand stand)) continue;
            double d = e.getLocation().distanceSquared(center);
            if (d < bestDist) {
                bestDist = d;
                nearest = stand;
            }
        }
        return nearest;
    }

    private void tickIncantation(UUID seanceUserUuid) {
        IncantationState state = incantations.get(seanceUserUuid);
        if (state == null) return;

        Player seanceUser = Bukkit.getPlayer(seanceUserUuid);
        if (seanceUser == null || !seanceUser.isOnline()) {
            cancelIncantation(seanceUserUuid, false);
            return;
        }

        Entity entity = Bukkit.getEntity(state.armorStandId);
        if (!(entity instanceof ArmorStand stand)) {
            cancelIncantation(seanceUserUuid, true);
            seanceUser.sendMessage(plugin.cfg().prefix() + "§cThe armor stand was removed! Incantation cancelled.");
            return;
        }

        if (seanceUser.getLocation().distance(stand.getLocation()) > 5.0) {
            cancelIncantation(seanceUserUuid, true);
            seanceUser.sendMessage(plugin.cfg().prefix() + "§cYou moved too far from the armor stand! Incantation cancelled.");
            return;
        }

        state.ticksRemaining--;

        int secondsLeft = (int) Math.ceil(state.ticksRemaining / 20.0);
        String bar = buildProgressBar(state.ticksRemaining, 1200);
        seanceUser.sendActionBar("§5§lSéance §r§7" + bar + " §d" + secondsLeft + "s");

        if (state.ticksRemaining <= 0) {
            completeReincarnation(seanceUserUuid, stand, state);
        }
    }

    private String buildProgressBar(int remaining, int total) {
        int filled = (int) ((double)(total - remaining) / total * 20);
        StringBuilder sb = new StringBuilder("§d");
        for (int i = 0; i < 20; i++) {
            if (i == filled) sb.append("§7");
            sb.append("|");
        }
        return sb.toString();
    }

    private void completeReincarnation(UUID seanceUserUuid, ArmorStand stand, IncantationState state) {
        cancelIncantation(seanceUserUuid, false);

        Player seanceUser = Bukkit.getPlayer(seanceUserUuid);
        Location loc = state.armorStandLocation;

        stand.remove();

        plugin.data().load(state.deadPlayerUuid);
        PlayerProfile deadProfile = plugin.data().get(state.deadPlayerUuid);
        deadProfile.permaDead = false;
        deadProfile.techniqueId = state.techniqueId != null ? state.techniqueId : deadProfile.permaDeadTechniqueId;
        deadProfile.permaDeadTechniqueId = null;
        deadProfile.isReincarnated = true;
        deadProfile.seanceSpawnWorld = loc.getWorld().getName();
        deadProfile.seanceSpawnX = loc.getX() + 0.5;
        deadProfile.seanceSpawnY = loc.getY();
        deadProfile.seanceSpawnZ = loc.getZ() + 0.5;
        plugin.data().save(state.deadPlayerUuid);

        PlayerProfile seanceProf = plugin.data().get(seanceUserUuid);
        seanceProf.seanceReincarnatedUuid = state.deadPlayerUuid.toString();
        plugin.data().save(seanceUserUuid);

        unbanPlayer(state.deadPlayerUuid);

        if (seanceUser != null) {
            seanceUser.sendMessage(plugin.cfg().prefix() + "§5§lThe séance is complete! §r§f" +
                    Bukkit.getOfflinePlayer(state.deadPlayerUuid).getName() + " §5has been reincarnated. They can now rejoin.");
        }

        Player deadPlayer = Bukkit.getPlayer(state.deadPlayerUuid);
        if (deadPlayer != null && deadPlayer.isOnline()) {
            deadPlayer.teleport(loc.clone().add(0.5, 0, 0.5));
            deadPlayer.setGameMode(GameMode.SURVIVAL);
            deadPlayer.setHealth(deadPlayer.getMaxHealth());
            deadPlayer.setFoodLevel(20);
            deadPlayer.sendMessage(plugin.cfg().prefix() + "§5§lYou have been reincarnated! Fight wisely — your soul is still bound.");
        }

        SeanceState seanceState = new SeanceState();
        seanceState.reincarnatedUuid = state.deadPlayerUuid;
        seances.put(seanceUserUuid, seanceState);
        reincarnatedByMap.put(state.deadPlayerUuid, seanceUserUuid);

        startDrain(seanceUserUuid, seanceState);
    }

    private void cancelIncantation(UUID seanceUserUuid, boolean message) {
        IncantationState state = incantations.remove(seanceUserUuid);
        if (state == null) return;
        if (state.taskId != -1) {
            Bukkit.getScheduler().cancelTask(state.taskId);
        }
        if (message) {
            Player p = Bukkit.getPlayer(seanceUserUuid);
            if (p != null) p.sendActionBar("");
        }
    }

    private void startDrain(UUID seanceUserUuid, SeanceState state) {
        if (state.drainTaskId != -1) Bukkit.getScheduler().cancelTask(state.drainTaskId);

        int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            SeanceState current = seances.get(seanceUserUuid);
            if (current == null) return;
            if (current.bindingVowActive) {
                Bukkit.getScheduler().cancelTask(current.drainTaskId);
                current.drainTaskId = -1;
                return;
            }

            Player seanceUser = Bukkit.getPlayer(seanceUserUuid);
            if (seanceUser == null || !seanceUser.isOnline()) return;

            int ce = plugin.ce().get(seanceUserUuid);
            if (ce <= 0) {
                endSeance(seanceUserUuid);
            } else {
                plugin.ce().tryConsume(seanceUserUuid, 1);
            }
        }, 5L, 5L);

        state.drainTaskId = taskId;
    }

    public boolean applyBindingVow(Player seanceUser) {
        UUID uuid = seanceUser.getUniqueId();
        SeanceState state = seances.get(uuid);
        if (state == null) {
            seanceUser.sendMessage(plugin.cfg().prefix() + "§cYou don't have an active séance.");
            return false;
        }
        if (state.bindingVowActive) {
            seanceUser.sendMessage(plugin.cfg().prefix() + "§cA binding vow is already active.");
            return false;
        }

        state.bindingVowActive = true;
        if (state.drainTaskId != -1) {
            Bukkit.getScheduler().cancelTask(state.drainTaskId);
            state.drainTaskId = -1;
        }

        PlayerProfile prof = plugin.data().get(uuid);
        prof.seanceBindingVowActive = true;
        plugin.data().save(uuid);

        seanceUser.sendMessage(plugin.cfg().prefix() + "§5§lBinding Vow activated! §r§f" +
                Bukkit.getOfflinePlayer(state.reincarnatedUuid).getName() +
                " §7will remain until they fall again. §cYou can no longer deal damage.");

        Player reincarnated = Bukkit.getPlayer(state.reincarnatedUuid);
        if (reincarnated != null) {
            reincarnated.sendMessage(plugin.cfg().prefix() + "§5§lA binding vow seals your soul. You are permanently reincarnated.");
        }
        return true;
    }

    public void endSeance(UUID seanceUserUuid) {
        SeanceState state = seances.remove(seanceUserUuid);
        if (state == null) return;

        if (state.drainTaskId != -1) {
            Bukkit.getScheduler().cancelTask(state.drainTaskId);
        }

        reincarnatedByMap.remove(state.reincarnatedUuid);

        PlayerProfile seanceProf = plugin.data().get(seanceUserUuid);
        seanceProf.seanceReincarnatedUuid = null;
        seanceProf.seanceBindingVowActive = false;
        plugin.data().save(seanceUserUuid);

        Player seanceUser = Bukkit.getPlayer(seanceUserUuid);
        if (seanceUser != null) {
            seanceUser.sendMessage(plugin.cfg().prefix() + "§cYour cursed energy is depleted. The séance has ended.");
        }

        Player reincarnated = Bukkit.getPlayer(state.reincarnatedUuid);
        plugin.data().load(state.reincarnatedUuid);
        PlayerProfile rProf = plugin.data().get(state.reincarnatedUuid);
        rProf.isReincarnated = false;
        rProf.permaDead = true;
        rProf.permaDeadTechniqueId = rProf.techniqueId;
        rProf.techniqueId = null;
        plugin.data().save(state.reincarnatedUuid);

        if (reincarnated != null && reincarnated.isOnline()) {
            banPermadeadPlayer(reincarnated);
        }
    }

    public void cancelSeance(Player seanceUser) {
        UUID uuid = seanceUser.getUniqueId();

        if (incantations.containsKey(uuid)) {
            cancelIncantation(uuid, true);
            seanceUser.sendMessage(plugin.cfg().prefix() + "§7Incantation cancelled.");
            return;
        }

        if (!seances.containsKey(uuid)) {
            seanceUser.sendMessage(plugin.cfg().prefix() + "§cYou don't have an active séance to cancel.");
            return;
        }

        endSeance(uuid);
    }

    public void onSeanceUserDeath(UUID seanceUserUuid) {
        SeanceState state = seances.get(seanceUserUuid);
        if (state == null) return;
        if (state.drainTaskId != -1) {
            Bukkit.getScheduler().cancelTask(state.drainTaskId);
            state.drainTaskId = -1;
        }
    }

    public void onSeanceUserRespawn(UUID seanceUserUuid) {
        SeanceState state = seances.get(seanceUserUuid);
        if (state == null) return;
        if (!state.bindingVowActive && state.drainTaskId == -1) {
            startDrain(seanceUserUuid, state);
        }
    }

    public void onReincarnatedPlayerDeath(UUID reincarnatedUuid) {
        UUID seanceUserUuid = reincarnatedByMap.remove(reincarnatedUuid);
        if (seanceUserUuid == null) return;

        SeanceState state = seances.remove(seanceUserUuid);
        if (state != null && state.drainTaskId != -1) {
            Bukkit.getScheduler().cancelTask(state.drainTaskId);
        }

        PlayerProfile seanceProf = plugin.data().get(seanceUserUuid);
        seanceProf.seanceBindingVowActive = false;
        seanceProf.seanceReincarnatedUuid = null;
        plugin.data().save(seanceUserUuid);

        Player seanceUser = Bukkit.getPlayer(seanceUserUuid);
        if (seanceUser != null) {
            seanceUser.sendMessage(plugin.cfg().prefix() + "§5The reincarnated soul has fallen. Your binding vow is released.");
        }

        PlayerProfile rProf = plugin.data().get(reincarnatedUuid);
        rProf.isReincarnated = false;
        plugin.data().save(reincarnatedUuid);
    }

    public String getStatus(UUID seanceUserUuid) {
        if (incantations.containsKey(seanceUserUuid)) {
            IncantationState state = incantations.get(seanceUserUuid);
            int secs = (int) Math.ceil(state.ticksRemaining / 20.0);
            return "§5Incantation in progress §7(" + secs + "s remaining)";
        }
        SeanceState state = seances.get(seanceUserUuid);
        if (state == null) return "§7No active séance.";

        String name = Bukkit.getOfflinePlayer(state.reincarnatedUuid).getName();
        String bindVow = state.bindingVowActive ? " §c[Binding Vow Active]" : "";
        int ce = plugin.ce().get(seanceUserUuid);
        return "§5Active séance §7— §fReincarnated: §d" + name + " §7| CE: §b" + ce + bindVow;
    }

    public boolean hasActiveSeance(UUID seanceUserUuid) { return seances.containsKey(seanceUserUuid); }
    public boolean hasActiveIncantation(UUID seanceUserUuid) { return incantations.containsKey(seanceUserUuid); }
    public boolean isReincarnatedBy(UUID reincarnatedUuid) { return reincarnatedByMap.containsKey(reincarnatedUuid); }
    public UUID getSeanceUserFor(UUID reincarnatedUuid) { return reincarnatedByMap.get(reincarnatedUuid); }
    public boolean hasBindingVowActive(UUID seanceUserUuid) {
        SeanceState s = seances.get(seanceUserUuid);
        return s != null && s.bindingVowActive;
    }

    public ItemStack createBindingVowItem() {
        ItemStack item = new ItemStack(Material.CHAIN);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§5§lBinding Vow");
            List<String> lore = new ArrayList<>();
            lore.add("§7A vow that transcends death itself.");
            lore.add("§7Right-click to activate.");
            lore.add("");
            lore.add("§c⚠ Trade-off: You cannot deal damage");
            lore.add("§cuntil the reincarnated soul falls again.");
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.getPersistentDataContainer().set(KEY_BINDING_VOW, PersistentDataType.INTEGER, 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    public boolean isBindingVow(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        Integer v = item.getItemMeta().getPersistentDataContainer().get(KEY_BINDING_VOW, PersistentDataType.INTEGER);
        return v != null && v == 1;
    }

    public ArmorStand findNearestArmorStandWithBody(Location center, double radius) {
        return findNearestArmorStand(center, radius);
    }
}