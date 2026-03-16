package me.axebanz.jJK;

import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class CmdTenShadows implements CommandExecutor, TabCompleter {

    private final JJKCursedToolsPlugin plugin;
    private final TenShadowsManager manager;

    public CmdTenShadows(JJKCursedToolsPlugin plugin, TenShadowsManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(plugin.cfg().prefix() + "§cPlayers only.");
            return true;
        }

        String assignedId = plugin.techniqueManager().getAssignedId(p.getUniqueId());
        if (!"ten_shadows".equalsIgnoreCase(assignedId)) {
            p.sendMessage(plugin.cfg().prefix() + "§cYou don't have §8Ten Shadows Technique§c equipped.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(p, label);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "summon" -> {
                if (args.length < 2) {
                    // Summon the currently selected shikigami from scroll wheel
                    TenShadowsProfile prof2 = manager.getProfile(p.getUniqueId());
                    TenShadowsSelectionUI ui = new TenShadowsSelectionUI(plugin);
                    ShikigamiType selected = ui.getSelected(prof2);
                    if (selected == null) {
                        p.sendMessage(plugin.cfg().prefix() + "§cNo shikigami selected. Use Shift + Scroll to select.");
                        return true;
                    }
                    if (prof2.isUnlocked(selected)) {
                        manager.trySummon(p, selected);
                    } else if (selected.requiresRitual() && !prof2.isDestroyed(selected)) {
                        manager.startRitual(p, selected);
                    } else {
                        p.sendMessage(plugin.cfg().prefix() + "§c" + selected.displayName() + " §cis locked. Start a ritual first.");
                    }
                    return true;
                }
                ShikigamiType type = ShikigamiType.from(args[1]);
                if (type == null) {
                    p.sendMessage(plugin.cfg().prefix() + "§cUnknown shikigami: §f" + args[1]);
                    p.sendMessage(plugin.cfg().prefix() + "§7Available: " + getShikigamiList());
                    return true;
                }
                manager.trySummon(p, type);
                return true;
            }
            case "store" -> {
                // Open shadow storage GUI
                manager.openShadowStorage(p);
                return true;
            }
            case "dismiss" -> {
                manager.dismiss(p);
                return true;
            }
            case "ritual" -> {
                if (args.length < 2) {
                    p.sendMessage(plugin.cfg().prefix() + "§cUsage: /" + label + " ritual <shikigami>");
                    return true;
                }
                ShikigamiType type = ShikigamiType.from(args[1]);
                if (type == null) {
                    p.sendMessage(plugin.cfg().prefix() + "§cUnknown shikigami: §f" + args[1]);
                    return true;
                }
                manager.startRitual(p, type);
                return true;
            }
            case "cancel" -> {
                manager.cancelRitual(p);
                return true;
            }
            case "list" -> {
                TenShadowsProfile prof = manager.getProfile(p.getUniqueId());
                p.sendMessage(plugin.cfg().prefix() + "§8§l═══ Ten Shadows ═══");
                for (ShikigamiType type : ShikigamiType.values()) {
                    ShikigamiState state = prof.getState(type);
                    String stateStr = switch (state) {
                        case LOCKED -> "§c✖ Locked";
                        case UNLOCKED -> "§a✔ Unlocked";
                        case ACTIVE -> "§b★ Active";
                        case DESTROYED -> "§4✖ Destroyed";
                        case FUSED_UNLOCKED -> "§6✦ Fused";
                    };
                    p.sendMessage("  " + type.displayName() + " §8— " + stateStr);
                }
                return true;
            }
            case "status" -> {
                TenShadowsProfile prof = manager.getProfile(p.getUniqueId());
                p.sendMessage(plugin.cfg().prefix() + "§8Ten Shadows Status:");
                if (prof.activeSummonId != null) {
                    ShikigamiType active = ShikigamiType.from(prof.activeSummonId);
                    p.sendMessage("§7Active: " + (active != null ? active.displayName() : "None"));
                } else {
                    p.sendMessage("§7Active: §fNone");
                }
                p.sendMessage("§7Ritual: " + (prof.ritualActive ? "§cIn Progress (" + prof.ritualTargetId + ")" : "§fNone"));
                int unlocked = prof.getUnlockedShikigami().size();
                p.sendMessage("§7Unlocked: §f" + unlocked + "/" + ShikigamiType.values().length);
                return true;
            }
            default -> {
                sendHelp(p, label);
                return true;
            }
        }
    }

    private void sendHelp(Player p, String label) {
        String pref = plugin.cfg().prefix();
        p.sendMessage(pref + "§8§lTen Shadows Technique:");
        p.sendMessage("§f/" + label + " summon [shikigami] §7— Summon selected/named shikigami");
        p.sendMessage("§f/" + label + " dismiss §7— Dismiss current shikigami");
        p.sendMessage("§f/" + label + " store §7— Open shadow storage");
        p.sendMessage("§f/" + label + " ritual <shikigami> §7— Start a taming ritual");
        p.sendMessage("§f/" + label + " cancel §7— Cancel active ritual");
        p.sendMessage("§f/" + label + " list §7— Show all shikigami and their status");
        p.sendMessage("§f/" + label + " status §7— Show current state");
    }

    private String getShikigamiList() {
        StringBuilder sb = new StringBuilder();
        for (ShikigamiType t : ShikigamiType.values()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(t.id());
        }
        return sb.toString();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!(sender instanceof Player p)) return List.of();
        String assignedId = plugin.techniqueManager().getAssignedId(p.getUniqueId());
        if (!"ten_shadows".equalsIgnoreCase(assignedId)) return List.of();

        if (args.length == 1) {
            return List.of("summon", "dismiss", "store", "ritual", "cancel", "list", "status");
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if ("summon".equals(sub)) {
                TenShadowsProfile prof = manager.getProfile(p.getUniqueId());
                List<String> result = new ArrayList<>();
                for (ShikigamiType type : prof.getUnlockedShikigami()) {
                    result.add(type.id());
                }
                return result;
            }
            if ("ritual".equals(sub)) {
                TenShadowsProfile prof = manager.getProfile(p.getUniqueId());
                List<String> result = new ArrayList<>();
                for (ShikigamiType type : prof.getLockedShikigami()) {
                    if (type.requiresRitual()) result.add(type.id());
                }
                return result;
            }
        }

        return List.of();
    }
}