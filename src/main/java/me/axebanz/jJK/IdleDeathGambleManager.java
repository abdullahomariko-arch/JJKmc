package me.axebanz.jJK;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class IdleDeathGambleManager {

    public static final int MAX_SPINS = 10;
    private static final long JACKPOT_DURATION_TICKS = 5020L; // 251 seconds (4:11)
    private static final long SPIN_COOLDOWN_TICKS = 40L; // 2 seconds between spins (or 20 if advanced)

    private final JJKCursedToolsPlugin plugin;
    private final Random random = new Random();

    // Per-player game state
    private final Map<UUID, IDGGameState> gameStates = new ConcurrentHashMap<>();

    public IdleDeathGambleManager(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    private IDGGameState getOrCreate(UUID uuid) {
        return gameStates.computeIfAbsent(uuid, IDGGameState::new);
    }

    /** Called when domain starts for a player */
    public void startGame(Player p) {
        IDGGameState state = getOrCreate(p.getUniqueId());
        state.reset();
        state.domainActive = true;
        state.lastSpinTick = -SPIN_COOLDOWN_TICKS; // allow immediate spin

        // Give pachinko lever item
        ItemStack lever = new ItemStack(org.bukkit.Material.LEVER);
        ItemMeta meta = lever.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("★ Pachinko Lever", NamedTextColor.GOLD));
            lever.setItemMeta(meta);
        }
        p.getInventory().setItem(8, lever);

        p.sendMessage(plugin.cfg().prefix() + "§6★ IDLE DEATH GAMBLE §8| §fDomain Activated!");
        p.sendMessage(plugin.cfg().prefix() + "§7Right-click the §6Pachinko Lever §7to spin! §8(10 spins)");

        IdleDeathGambleUI.showGameUI(p, state);
    }

    /** Execute one spin for the player */
    public void spin(Player p) {
        IDGGameState state = gameStates.get(p.getUniqueId());
        if (state == null || !state.domainActive) {
            p.sendMessage(plugin.cfg().prefix() + "§cNo active domain game!");
            return;
        }

        if (state.jackpotActive) {
            p.sendMessage(plugin.cfg().prefix() + "§6★ JACKPOT is already active!");
            return;
        }

        long currentTick = plugin.getServer().getCurrentTick();
        long spinCooldown = state.fasterSpins ? 20L : SPIN_COOLDOWN_TICKS;
        if (currentTick - state.lastSpinTick < spinCooldown) {
            long remaining = (spinCooldown - (currentTick - state.lastSpinTick)) / 20;
            p.sendMessage(plugin.cfg().prefix() + "§cWait " + (remaining + 1) + "s before next spin.");
            return;
        }

        if (state.spinCount >= MAX_SPINS) {
            p.sendMessage(plugin.cfg().prefix() + "§cOut of spins! Domain collapsing...");
            plugin.domainManager().collapse(p);
            return;
        }

        state.lastSpinTick = currentTick;
        state.spinCount++;

        // Generate 3 random numbers 1-7
        int n1 = random.nextInt(7) + 1;
        int n2 = random.nextInt(7) + 1;
        int n3 = random.nextInt(7) + 1;
        state.lastRoll = new int[]{n1, n2, n3};

        // Show spin results
        p.sendTitle(
                "§6[ " + n1 + " ] §e[ " + n2 + " ] §6[ " + n3 + " ]",
                "§7Spin " + state.spinCount + "/" + MAX_SPINS,
                5, 40, 10
        );
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);

        // Check for riichi (2 matching)
        boolean riichi = (n1 == n2 && n1 != n3) || (n1 == n3 && n1 != n2) || (n2 == n3 && n1 != n2);
        boolean threeMatch = (n1 == n2 && n2 == n3);

        if (threeMatch) {
            activateJackpot(p, state, n1);
            return;
        }

        // Apply random indicator from this spin
        applyIndicator(p, state);

        if (riichi) {
            int scenario = random.nextInt(4);
            triggerRiichi(p, state, scenario);
        } else {
            IdleDeathGambleUI.showGameUI(p, state);

            if (state.spinCount >= MAX_SPINS) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (state.domainActive && !state.jackpotActive) {
                        p.sendMessage(plugin.cfg().prefix() + "§cNo jackpot — Domain collapses.");
                        plugin.domainManager().collapse(p);
                    }
                }, 60L);
            }
        }
    }

    private void applyIndicator(Player p, IDGGameState state) {
        int roll = random.nextInt(10);
        if (roll < 4) {
            state.greenIndicators++;
            p.sendMessage("§a[DOOR] §7Green indicator! +2♥ damage on next hit.");
        } else if (roll < 7) {
            state.redIndicators++;
            p.sendMessage("§c[BALL] §7Red indicator! Knockback projectile loaded.");
        } else if (roll < 9) {
            state.goldIndicators++;
            state.riichiBonus += 5;
            p.sendMessage("§6[GOLD] §7Gold indicator! +5% jackpot chance.");
        } else {
            state.rainbowIndicator = true;
            p.sendMessage("§d[RAINBOW] §dRAINBOW indicator! §7Next riichi is GUARANTEED!");
        }
    }

    public void triggerRiichi(Player p, IDGGameState state, int scenario) {
        String[] scenarioNames = {
                "Transit Card Riichi",
                "Seat Struggle Riichi",
                "Potty Emergency Riichi",
                "Friday Night Final Train Riichi"
        };
        String[] scenarioStories = {
                "§eYuki must pass through the gate on time!",
                "§eYuki must find a seat!",
                "§eHiro must reach the station in time!",
                "§eWill Yume reappear after the train departs?"
        };
        int[] scenarioChances = { 30, 50, 50, 80 };

        p.sendTitle("§6§l★ RIICHI ★", "§e" + scenarioNames[scenario], 10, 60, 10);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.2f);

        Bukkit.getScheduler().runTaskLater(plugin, () -> p.sendTitle(
                "§6" + scenarioNames[scenario], scenarioStories[scenario], 5, 60, 10), 20L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> p.sendTitle(
                "§6" + scenarioNames[scenario], "§7Rolling...", 5, 40, 10), 60L);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            resolveRiichi(p, state, scenario, scenarioChances[scenario]);
        }, 100L);
    }

    private void resolveRiichi(Player p, IDGGameState state, int scenario, int baseChance) {
        if (!state.domainActive) return;

        int totalChance = baseChance + state.riichiBonus;
        if (state.rainbowIndicator) totalChance = 100;

        boolean jackpot = random.nextInt(100) < totalChance;

        if (jackpot) {
            int matchNum = state.lastRoll[0];
            activateJackpot(p, state, matchNum);
        } else {
            p.sendTitle("§c§lFAILED", "§7Back to normal mode...", 10, 40, 10);
            state.rainbowIndicator = false;
            IdleDeathGambleUI.showGameUI(p, state);

            if (state.spinCount >= MAX_SPINS) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (state.domainActive && !state.jackpotActive) {
                        p.sendMessage(plugin.cfg().prefix() + "§cNo jackpot — Domain collapses.");
                        plugin.domainManager().collapse(p);
                    }
                }, 60L);
            }
        }
    }

    public void activateJackpot(Player p, IDGGameState state, int matchNum) {
        state.jackpotActive = true;
        state.jackpotStartTick = plugin.getServer().getCurrentTick();
        state.jackpotMatchNum = matchNum;
        state.domainActive = false;

        // Collapse domain
        plugin.domainManager().collapse(p);

        // Jackpot effects
        p.sendTitle("§6§l★ JACKPOT ★", "§eUnlimited CE + Auto-Heal!", 10, 80, 20);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 2.0f, 2.0f);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 1.0f);

        spawnJackpotFireworks(p);

        int durationTicks = (int) JACKPOT_DURATION_TICKS;
        p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, durationTicks, 4, false, false, true));
        p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, durationTicks, 3, false, false, true));

        // Clear any existing expand cooldown NOW so it's ready when jackpot ends
        plugin.cooldowns().setCooldown(p.getUniqueId(), "idg.expand", 0);

        // Start jackpot CE maintenance task
        state.jackpotTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            tickJackpot(p, state);
        }, 0L, 1L);

        p.sendMessage(plugin.cfg().prefix() + "§6§l★ JACKPOT ★ §7Unlimited CE + Auto-Heal for §f4:11§7!");
    }

    private void spawnJackpotFireworks(Player p) {
        Location loc = p.getLocation();
        for (int i = 0; i < 5; i++) {
            final int delay = i * 10;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Firework fw = loc.getWorld().spawn(loc.clone().add(
                        (random.nextDouble() - 0.5) * 4,
                        1,
                        (random.nextDouble() - 0.5) * 4
                ), Firework.class);
                FireworkMeta meta = fw.getFireworkMeta();
                meta.addEffect(FireworkEffect.builder()
                        .with(FireworkEffect.Type.STAR)
                        .withColor(Color.YELLOW, Color.ORANGE, Color.RED)
                        .withFade(Color.WHITE)
                        .withFlicker()
                        .withTrail()
                        .build());
                meta.setPower(1);
                fw.setFireworkMeta(meta);
            }, delay);
        }
    }

    public void tickJackpot(Player p, IDGGameState state) {
        long elapsed = plugin.getServer().getCurrentTick() - state.jackpotStartTick;

        if (elapsed >= JACKPOT_DURATION_TICKS) {
            endJackpot(p, state);
            return;
        }

        // Maintain max CE every tick
        UUID u = p.getUniqueId();
        int maxCE = plugin.ce().max(u);
        PlayerProfile prof = plugin.data().get(u);
        if (prof.ce < maxCE) {
            prof.ce = maxCE;
            plugin.data().save(u);
        }

        // Update UI every 20 ticks
        if (elapsed % 20 == 0) {
            IdleDeathGambleUI.showJackpotUI(p, state, JACKPOT_DURATION_TICKS - elapsed);
            p.getWorld().playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f,
                    1.0f + (float)(elapsed % 40) / 40.0f);
        }
    }

    public void endJackpot(Player p, IDGGameState state) {
        if (state.jackpotTaskId != -1) {
            Bukkit.getScheduler().cancelTask(state.jackpotTaskId);
            state.jackpotTaskId = -1;
        }
        state.jackpotActive = false;

        // Remove healing effects
        p.removePotionEffect(PotionEffectType.REGENERATION);
        p.removePotionEffect(PotionEffectType.ABSORPTION);

        // Determine next domain bonuses based on jackpot match number
        if (state.jackpotMatchNum % 2 != 0) {
            state.advancedProbability = true;
            state.riichiBonus = 15;
            p.sendMessage(plugin.cfg().prefix() + "§6Jackpot ended! §eAdvanced Probability §7mode for next domain.");
        } else {
            state.fasterSpins = true;
            p.sendMessage(plugin.cfg().prefix() + "§6Jackpot ended! §eFaster Spins §7mode for next domain.");
        }

        // Zero cooldown — can reopen immediately
        plugin.cooldowns().setCooldown(p.getUniqueId(), "idg.expand", 0);

        p.sendTitle("§6Jackpot Ended", "§7Re-expanding domain...", 10, 40, 20);
        p.sendActionBar("§6Jackpot Over §8| §aRe-launching domain!");

        // Auto-relaunch domain after 3 seconds (60 ticks)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!p.isOnline()) return;

            // Make sure no domain is already active
            if (plugin.domainManager().getDomain(p) != null) return;

            // Make sure cooldown is definitely clear
            plugin.cooldowns().setCooldown(p.getUniqueId(), "idg.expand", 0);

            // Re-expand
            IdleDeathGambleDomain domain = new IdleDeathGambleDomain(plugin, p, this);
            plugin.domainManager().expand(p, domain);
            startGame(p);
            p.sendMessage(plugin.cfg().prefix() + "§6★ Domain re-expanded automatically!");
        }, 60L);
    }

    /** Called every domain tick (from IdleDeathGambleDomain.onTick) */
    public void onDomainTick(Player p) {
        IDGGameState state = gameStates.get(p.getUniqueId());
        if (state == null || !state.domainActive) return;

        long tick = plugin.getServer().getCurrentTick();
        if (tick % 20 == 0) {
            IdleDeathGambleUI.showGameUI(p, state);
        }
    }

    /** Called when domain collapses (from IdleDeathGambleDomain.onDomainEnd) */
    public void onDomainCollapsed(Player p) {
        IDGGameState state = gameStates.get(p.getUniqueId());
        if (state != null) {
            state.domainActive = false;
            removeLevers(p);
        }
    }

    private void removeLevers(Player p) {
        for (int i = 0; i < p.getInventory().getSize(); i++) {
            ItemStack item = p.getInventory().getItem(i);
            if (item != null && item.getType() == org.bukkit.Material.LEVER) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null && meta.hasDisplayName()) {
                    p.getInventory().setItem(i, null);
                }
            }
        }
    }

    public boolean isJackpotActive(UUID uuid) {
        IDGGameState state = gameStates.get(uuid);
        return state != null && state.jackpotActive;
    }

    public IDGGameState getState(UUID uuid) {
        return gameStates.get(uuid);
    }

    public long getJackpotTimeRemaining(UUID uuid) {
        IDGGameState state = gameStates.get(uuid);
        if (state == null || !state.jackpotActive) return 0;
        long elapsed = plugin.getServer().getCurrentTick() - state.jackpotStartTick;
        return Math.max(0, JACKPOT_DURATION_TICKS - elapsed);
    }

    public void cleanup(UUID uuid) {
        IDGGameState state = gameStates.remove(uuid);
        if (state != null && state.jackpotTaskId != -1) {
            Bukkit.getScheduler().cancelTask(state.jackpotTaskId);
        }
    }

    // ===== Inner State Class =====
    public static final class IDGGameState {
        public final UUID uuid;
        public boolean domainActive = false;
        public boolean jackpotActive = false;
        public int spinCount = 0;
        public int[] lastRoll = {0, 0, 0};
        public int greenIndicators = 0;
        public int redIndicators = 0;
        public int goldIndicators = 0;
        public boolean rainbowIndicator = false;
        public int riichiBonus = 0;
        public int jackpotMatchNum = 0;
        public long jackpotStartTick = 0;
        public int jackpotTaskId = -1;
        public long lastSpinTick = -40L;
        public boolean advancedProbability = false;
        public boolean fasterSpins = false;

        public IDGGameState(UUID uuid) {
            this.uuid = uuid;
        }

        public void reset() {
            domainActive = false;
            jackpotActive = false;
            spinCount = 0;
            lastRoll = new int[]{0, 0, 0};
            greenIndicators = 0;
            redIndicators = 0;
            goldIndicators = 0;
            rainbowIndicator = false;
            // Keep riichiBonus, advancedProbability, fasterSpins from previous jackpot
        }
    }
}