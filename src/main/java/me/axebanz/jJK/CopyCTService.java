package me.axebanz.jJK;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class CopyCTService {

    private final JJKCursedToolsPlugin plugin;

    public CopyCTService(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * /copy ct — use the currently selected copied technique.
     * Consumes 1 charge. When charges hit 0, that technique is removed.
     */
    public void tryUseCopiedTechnique(Player p) {
        if (!plugin.copy().canUseCopy(p)) {
            p.sendMessage(plugin.cfg().prefix() + "§cYou must have §dCopy§c equipped.");
            return;
        }

        if (!plugin.techniqueManager().canUseTechniqueActions(p, true)) return;

        String key = "copy.ct.use";

        if (plugin.cooldowns().isOnCooldown(p.getUniqueId(), key)) {
            long rem = plugin.cooldowns().remainingSeconds(p.getUniqueId(), key);
            plugin.actionbarUI().setTimer(p.getUniqueId(), key, "■", "§d", rem);
            return;
        }

        PlayerProfile prof = plugin.data().get(p.getUniqueId());
        if (prof.copiedTechniqueId == null) {
            p.sendMessage(plugin.cfg().prefix() + "§cYou have not copied any technique. Feed Cursed Bodies to Rika.");
            return;
        }

        // Check charges
        int charges = prof.copyCharges.getOrDefault(prof.copiedTechniqueId, 0);
        if (charges <= 0) {
            p.sendMessage(plugin.cfg().prefix() + "§cNo charges left for §f" + getDisplayName(prof.copiedTechniqueId) + "§c. Feed another Cursed Body to Rika.");
            // Auto-switch to next available technique
            autoSwitchToNext(prof);
            plugin.data().save(p.getUniqueId());
            return;
        }

        // Execute the technique
        boolean success = executeTechnique(p, prof.copiedTechniqueId);
        if (!success) return;

        // Consume 1 charge
        charges--;
        if (charges <= 0) {
            prof.copyCharges.remove(prof.copiedTechniqueId);
            p.sendMessage(plugin.cfg().prefix() + "§c" + getDisplayName(prof.copiedTechniqueId) + " §ccharges depleted!");

            // Auto-switch to next available technique
            autoSwitchToNext(prof);
        } else {
            prof.copyCharges.put(prof.copiedTechniqueId, charges);
            p.sendMessage(plugin.cfg().prefix() + "§7Charges remaining: §f" + charges + " §7(" + getDisplayName(prof.copiedTechniqueId) + ")");
        }

        plugin.data().save(p.getUniqueId());

        plugin.cooldowns().setCooldown(p.getUniqueId(), key, 15);
        plugin.actionbarUI().setTimer(p.getUniqueId(), key, "■", "§d", 15);

        p.playSound(p.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.6f, 1.6f);
    }

    /**
     * Shift + /copy ct — shuffle between copied techniques.
     * Called from CmdCopy when player is sneaking.
     */
    public void shuffleCopiedTechnique(Player p) {
        if (!plugin.copy().canUseCopy(p)) {
            p.sendMessage(plugin.cfg().prefix() + "§cYou must have §dCopy§c equipped.");
            return;
        }

        PlayerProfile prof = plugin.data().get(p.getUniqueId());

        // Get all techniques that have charges
        List<String> available = new ArrayList<>();
        for (var entry : prof.copyCharges.entrySet()) {
            if (entry.getValue() > 0) {
                available.add(entry.getKey());
            }
        }

        if (available.isEmpty()) {
            p.sendMessage(plugin.cfg().prefix() + "§cYou have no copied techniques. Feed Cursed Bodies to Rika.");
            return;
        }

        if (available.size() == 1) {
            prof.copiedTechniqueId = available.get(0);
            plugin.data().save(p.getUniqueId());
            int charges = prof.copyCharges.getOrDefault(prof.copiedTechniqueId, 0);
            p.sendMessage(plugin.cfg().prefix() + "§dCopy CT: §f" + getDisplayName(prof.copiedTechniqueId) + " §7(Charges: " + charges + ") §8— only one available");
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.8f);
            return;
        }

        // Find current index and go to next
        int currentIdx = -1;
        if (prof.copiedTechniqueId != null) {
            currentIdx = available.indexOf(prof.copiedTechniqueId);
        }

        int nextIdx = (currentIdx + 1) % available.size();
        prof.copiedTechniqueId = available.get(nextIdx);
        plugin.data().save(p.getUniqueId());

        int charges = prof.copyCharges.getOrDefault(prof.copiedTechniqueId, 0);
        p.sendMessage(plugin.cfg().prefix() + "§dCopy CT: §fSwitched to " + getDisplayName(prof.copiedTechniqueId) + " §7(Charges: " + charges + ")");
        p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 1.4f);

        // Show all available in a list
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < available.size(); i++) {
            String id = available.get(i);
            int ch = prof.copyCharges.getOrDefault(id, 0);
            boolean selected = id.equals(prof.copiedTechniqueId);
            sb.append(selected ? " §d▶ " : " §7• ");
            sb.append(selected ? "§f" : "§7");
            sb.append(getDisplayName(id));
            sb.append(" §8(").append(ch).append(")");
            if (i < available.size() - 1) sb.append("\n");
        }
        p.sendMessage(sb.toString());
    }

    private boolean executeTechnique(Player p, String techId) {
        switch (techId.toLowerCase()) {
            case "cursed_speech" -> {
                List<Runnable> actions = new ArrayList<>();
                actions.add(() -> plugin.cursedSpeech().castNoMove(p));
                actions.add(() -> plugin.cursedSpeech().castPlummet(p));
                actions.add(() -> plugin.cursedSpeech().castExplode(p));
                actions.get(new java.util.Random().nextInt(actions.size())).run();
                return true;
            }
            case "boogie_woogie" -> {
                plugin.boogieWoogie().clapSwap(p);
                return true;
            }
            case "creation" -> {
                List<Runnable> actions = new ArrayList<>();
                actions.add(() -> plugin.creationManager().shuffle(p));
                actions.add(() -> plugin.creationManager().tryCreate(p));
                actions.get(new java.util.Random().nextInt(actions.size())).run();
                return true;
            }
            case "gravity" -> {
                // Pick a random gravity ability slot
                AbilitySlot[] slots = AbilitySlot.values();
                AbilitySlot slot = slots[new java.util.Random().nextInt(slots.length)];
                Technique t = plugin.techniques().get("gravity");
                if (t != null) t.castAbility(p, slot);
                return true;
            }
            case "projection" -> {
                if (plugin.projectionManager() != null) {
                    plugin.projectionManager().tryActivate(p);
                }
                return true;
            }
            case "idle_death_gamble" -> {
                Technique t = plugin.techniques().get("idle_death_gamble");
                if (t != null) t.castAbility(p, AbilitySlot.ONE);
                return true;
            }
            case "seance" -> {
                if (plugin.seanceManager() != null) {
                    plugin.seanceManager().startIncantation(p);
                }
                return true;
            }
            case "strawdoll" -> {
                if (plugin.strawDollManager() != null) {
                    // Pick resonance or hairpin randomly
                    if (new java.util.Random().nextBoolean()) {
                        plugin.strawDollManager().activateResonance(p);
                    } else {
                        plugin.strawDollManager().activateHairpin(p);
                    }
                }
                return true;
            }
            case "copy" -> {
                // Copy of Copy — just notify
                p.sendMessage(plugin.cfg().prefix() + "§dYou can't copy Copy itself!");
                return false;
            }
            default -> {
                // Generic fallback: try to find the technique in the registry and cast slot 1
                Technique t = plugin.techniques().get(techId);
                if (t != null) {
                    t.castAbility(p, AbilitySlot.ONE);
                    return true;
                }
                p.sendMessage(plugin.cfg().prefix() + "§cCopied technique not supported yet: §f" + techId);
                return false;
            }
        }
    }

    private void autoSwitchToNext(PlayerProfile prof) {
        for (var entry : prof.copyCharges.entrySet()) {
            if (entry.getValue() > 0) {
                prof.copiedTechniqueId = entry.getKey();
                return;
            }
        }
        // No techniques left
        prof.copiedTechniqueId = null;
    }

    private String getDisplayName(String techId) {
        if (techId == null) return "None";
        Technique t = plugin.techniques().get(techId);
        return t != null ? t.displayName() : techId;
    }
}