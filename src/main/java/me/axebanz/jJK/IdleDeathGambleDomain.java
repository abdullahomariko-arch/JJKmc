package me.axebanz.jJK;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class IdleDeathGambleDomain extends DomainExpansion {

    private final IdleDeathGambleManager idgManager;

    /** Tracks players who have already received the domain explanation — they won't get it again */
    private final Set<UUID> alreadyExplained = new HashSet<>();

    public IdleDeathGambleDomain(JJKCursedToolsPlugin plugin, Player caster, IdleDeathGambleManager idgManager) {
        super(plugin, caster);
        this.refinement = 7.0;
        this.idgManager = idgManager;
    }

    @Override
    public String getName() {
        return "Idle Death Gamble: CR Private Pure Love Train Version 1/239";
    }

    @Override
    public int getRadius() {
        return 12;
    }

    @Override
    protected int getExpansionTickDelay() {
        return 2; // Slightly faster growth for IDG's smaller domain
    }

    @Override
    public void buildInterior() {
        World w = center.getWorld();
        if (w == null) return;

        int r = getRadius() - 2;
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();

        Material[] palette = {
                Material.WHITE_CONCRETE,
                Material.SNOW_BLOCK,
                Material.QUARTZ_BLOCK,
                Material.WHITE_WOOL
        };

        for (int x = cx - r; x <= cx + r; x++) {
            for (int z = cz - r; z <= cz + r; z++) {
                double dist = Math.sqrt((x - cx) * (x - cx) + (z - cz) * (z - cz));
                if (dist <= r) {
                    int floorY = cy - 1;
                    Location loc = new Location(w, x, floorY, z);
                    if (!savedBlocks.containsKey(loc)) {
                        savedBlocks.put(loc, loc.getBlock().getState());
                    }
                    int index = (Math.abs(x + z)) % palette.length;
                    loc.getBlock().setType(palette[index], false);
                }
            }
        }

        // Override barrier blocks with WHITE_CONCRETE for IDG's visual style
        // This runs AFTER the base class animated expansion places BARRIER blocks,
        // so we replace them with the white aesthetic
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!active) return;
            World world = center.getWorld();
            if (world == null) return;

            int fullR = getRadius();
            int ccx = center.getBlockX();
            int ccy = center.getBlockY();
            int ccz = center.getBlockZ();

            for (Location loc : barrierBlocks) {
                org.bukkit.block.Block block = loc.getBlock();
                block.setType(Material.WHITE_CONCRETE, false);
            }
        }, 2L);
    }

    @Override
    public void onSureHit(Player target) {
        UUID targetUuid = target.getUniqueId();

        // Only explain the domain ONCE per player — if they already got it, skip
        if (alreadyExplained.contains(targetUuid)) return;
        alreadyExplained.add(targetUuid);

        // Sure-hit: Domain explanation — shown once
        target.sendTitle(
                "§6§lIDLE DEATH GAMBLE",
                "§fCR Private Pure Love Train — Version 1/239",
                10, 80, 20
        );

        // Delayed lore messages
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!target.isOnline()) return;
            target.sendMessage("");
            target.sendMessage("§8§m                                                            ");
            target.sendMessage("");
            target.sendMessage("  §6§l★ DOMAIN EXPLANATION: IDLE DEATH GAMBLE §6§l★");
            target.sendMessage("");
            target.sendMessage("  §fYou are trapped within §6Hakari Kinji§f's domain.");
            target.sendMessage("  §7This domain takes the form of a §fpachinko parlor§7.");
            target.sendMessage("");
            target.sendMessage("  §7The caster will spin a §6Pachinko Lever §7up to §e10 times§7.");
            target.sendMessage("  §7Each spin rolls §f3 numbers §7from §e1-7§7.");
            target.sendMessage("");
            target.sendMessage("  §a▸ §f2 matching numbers §7= §eRIICHI §7(a scenario plays out)");
            target.sendMessage("  §a▸ §f3 matching numbers §7= §6§lINSTANT JACKPOT");
            target.sendMessage("");
            target.sendMessage("  §7During Riichi, a story scenario determines if");
            target.sendMessage("  §7the jackpot is hit. Indicators boost the odds:");
            target.sendMessage("  §a[DOOR] §7+2♥ damage  §c[BALL] §7Knockback  §6[GOLD] §7+5% chance  §d[RAINBOW] §7Guaranteed!");
            target.sendMessage("");
            target.sendMessage("  §6§lJACKPOT §7= §fUnlimited Cursed Energy §7+ §cAuto-Heal §7for §e4:11§7.");
            target.sendMessage("  §7If no jackpot after 10 spins — the domain collapses.");
            target.sendMessage("");
            target.sendMessage("  §c§lYou cannot escape until it ends.");
            target.sendMessage("");
            target.sendMessage("§8§m                                                            ");
            target.sendMessage("");
        }, 60L); // 3 second delay after the title

        // Sound effect for dramatic flair
        target.playSound(target.getLocation(), org.bukkit.Sound.BLOCK_END_PORTAL_SPAWN, 0.6f, 1.4f);
    }

    @Override
    public void onTick() {
        if (idgManager != null) {
            idgManager.onDomainTick(caster);
        }
    }

    @Override
    public void onDomainEnd() {
        if (idgManager != null) {
            idgManager.onDomainCollapsed(caster);
        }
    }
}