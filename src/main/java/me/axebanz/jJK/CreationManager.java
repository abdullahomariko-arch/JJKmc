package me.axebanz.jJK;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class CreationManager {

    private final JJKCursedToolsPlugin plugin;
    private final Map<UUID, CreationState> states = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastCreateTime = new ConcurrentHashMap<>();

    private static final long CREATE_COOLDOWN_MS = 5000;

    public CreationManager(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean hasTechnique(Player p) {
        Technique t = plugin.techniqueManager().getAssigned(p.getUniqueId());
        return t != null && "creation".equalsIgnoreCase(t.id());
    }

    /** Check if this player has Creation via Copy */
    public boolean hasCopiedTechnique(Player p) {
        if (!plugin.copy().canUseCopy(p)) return false;
        PlayerProfile prof = plugin.data().get(p.getUniqueId());
        return prof.copiedTechniqueId != null
                && prof.copiedTechniqueId.equalsIgnoreCase("creation");
    }

    /** Can use either natively or via Copy */
    public boolean canUse(Player p) {
        return hasTechnique(p) || hasCopiedTechnique(p);
    }

    public CreationState getOrCreate(UUID uuid) {
        return states.computeIfAbsent(uuid, CreationState::new);
    }

    public void shuffle(Player p) {
        CreationState state = getOrCreate(p.getUniqueId());
        state.shuffleNext();
        CreationCategory cat = state.getCurrentCategory();

        showMode(p, cat);
    }

    public boolean canCreate(UUID uuid) {
        long now = System.currentTimeMillis();
        long lastTime = lastCreateTime.getOrDefault(uuid, 0L);
        return (now - lastTime) >= CREATE_COOLDOWN_MS;
    }

    public long getRemainingCooldown(UUID uuid) {
        long now = System.currentTimeMillis();
        long lastTime = lastCreateTime.getOrDefault(uuid, 0L);
        long remaining = CREATE_COOLDOWN_MS - (now - lastTime);
        return Math.max(0, remaining / 1000);
    }

    public boolean tryCreate(Player p) {
        CreationState state = getOrCreate(p.getUniqueId());
        CreationCategory category = state.getCurrentCategory();

        if (!canCreate(p.getUniqueId())) {
            long remaining = getRemainingCooldown(p.getUniqueId());
            p.sendActionBar("§cCooldown: §f" + remaining + "s");
            p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.6f);
            return false;
        }

        if (!plugin.ce().tryConsume(p.getUniqueId(), category.ceCost())) {
            p.sendActionBar("§cNot enough Cursed Energy!");
            p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.6f);
            return false;
        }

        int amountToGive = category.amountGiven();

        if (category == CreationCategory.TOOLS_ARMOUR) {
            Set<Material> chosen = new HashSet<>();
            Material[] allItems = CreationInventory.getAllItems(category);
            Random rng = new Random();

            while (chosen.size() < 3 && chosen.size() < allItems.length) {
                Material randomMat = allItems[rng.nextInt(allItems.length)];
                chosen.add(randomMat);
            }

            for (Material mat : chosen) {
                ItemStack item = new ItemStack(mat, 1);
                p.getInventory().addItem(item);
            }

            showStackGain(p, category, String.join(" / ", chosen.stream().map(Material::name).toList()));
        } else {
            Material randomMaterial = CreationInventory.getRandomItem(category);
            ItemStack item = new ItemStack(randomMaterial, amountToGive);
            p.getInventory().addItem(item);

            showStackGain(p, category, randomMaterial.name());
        }

        lastCreateTime.put(p.getUniqueId(), System.currentTimeMillis());
        return true;
    }

    private void showMode(Player p, CreationCategory cat) {
        String icon = "■";
        String color = categoryColor(cat);

        p.sendActionBar(color + icon + " §fCREATION §8|§r " + color + cat.displayName());
        p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 1.2f);
    }

    private void showStackGain(Player p, CreationCategory category, String itemName) {
        String icon = "■";
        String color = categoryColor(category);

        p.sendActionBar(color + icon + " §fCREATED §8|§r " + color + itemName);
        p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 0.35f, 1.7f);
    }

    public String categoryColor(CreationCategory cat) {
        return switch (cat) {
            case BLOCKS -> "§9";
            case TOOLS_ARMOUR -> "§6";
            case CONSUMABLES -> "§a";
        };
    }

    public CreationCategory getCurrentCategory(UUID uuid) {
        return getOrCreate(uuid).getCurrentCategory();
    }

    public void clearState(UUID uuid) {
        states.remove(uuid);
        lastCreateTime.remove(uuid);
    }
}