package me.axebanz.jJK;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * /jjk keybind <ability> <key>   — binds an ability to a key
 * /jjk keybind list              — lists current keybinds
 * /jjk keybind clear <key>       — removes a keybind
 *
 * Supported keys: F, Q, SHIFT, RIGHT_CLICK, LEFT_CLICK
 * Example: /jjk keybind blue F
 */
public final class CmdKeybind implements SubCommand {

    private final JJKCursedToolsPlugin plugin;

    public CmdKeybind(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String name() { return "keybind"; }

    @Override
    public String permission() { return null; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(plugin.cfg().prefix() + "§cPlayers only.");
            return true;
        }

        KeybindManager mgr = plugin.keybindManager();
        if (mgr == null) {
            p.sendMessage(plugin.cfg().prefix() + "§cKeybind system not loaded.");
            return true;
        }

        // args[0] = "keybind", args[1] = sub-action, args[2...] = params
        if (args.length < 2) {
            sendHelp(p);
            return true;
        }

        String action = args[1].toLowerCase(Locale.ROOT);

        switch (action) {
            case "list" -> {
                Map<String, String> binds = mgr.getKeybinds(p);
                if (binds.isEmpty()) {
                    p.sendMessage(plugin.cfg().prefix() + "§7You have no keybinds set.");
                } else {
                    p.sendMessage(plugin.cfg().prefix() + "§bYour keybinds:");
                    for (Map.Entry<String, String> e : binds.entrySet()) {
                        p.sendMessage("  §f" + e.getKey() + " §7→ §b" + e.getValue());
                    }
                }
            }
            case "clear" -> {
                if (args.length < 3) {
                    p.sendMessage(plugin.cfg().prefix() + "§cUsage: /jjk keybind clear <key>");
                    return true;
                }
                String key = args[2].toUpperCase(Locale.ROOT);
                if (!KeybindManager.SUPPORTED_KEYS.contains(key)) {
                    p.sendMessage(plugin.cfg().prefix() + "§cInvalid key. Use: " + String.join(", ", KeybindManager.SUPPORTED_KEYS));
                    return true;
                }
                mgr.unbind(p, key);
                p.sendMessage(plugin.cfg().prefix() + "§7Removed keybind for key §f" + key + "§7.");
            }
            default -> {
                // treat action as the ability, args[2] as the key
                String ability = action;
                if (args.length < 3) {
                    p.sendMessage(plugin.cfg().prefix() + "§cUsage: /jjk keybind <ability> <key>");
                    p.sendMessage(plugin.cfg().prefix() + "§7Keys: " + String.join(", ", KeybindManager.SUPPORTED_KEYS));
                    return true;
                }
                String key = args[2].toUpperCase(Locale.ROOT);
                if (!KeybindManager.SUPPORTED_KEYS.contains(key)) {
                    p.sendMessage(plugin.cfg().prefix() + "§cInvalid key. Use: " + String.join(", ", KeybindManager.SUPPORTED_KEYS));
                    return true;
                }
                mgr.bind(p, ability, key);
                p.sendMessage(plugin.cfg().prefix() + "§aKeybind set: §f" + key.toUpperCase(Locale.ROOT)
                        + " §7→ §b" + ability.toLowerCase(Locale.ROOT));
            }
        }
        return true;
    }

    @Override
    public List<String> tab(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) return List.of();

        // args here are shifted (jjk keybind ...) so args[0] = sub-action
        if (args.length == 1) {
            List<String> options = new ArrayList<>(List.of("list", "clear",
                    "infinity", "blue", "bluemax", "red", "redmax", "purple", "nuke", "void"));
            return prefixFilter(options, args[0]);
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("clear")) {
                return prefixFilter(new ArrayList<>(KeybindManager.SUPPORTED_KEYS), args[1]);
            }
            if (!sub.equals("list")) {
                return prefixFilter(new ArrayList<>(KeybindManager.SUPPORTED_KEYS), args[1]);
            }
        }
        return List.of();
    }

    private void sendHelp(Player p) {
        p.sendMessage(plugin.cfg().prefix() + "§bKeybind System:");
        p.sendMessage("  §f/jjk keybind <ability> <key> §7— bind an ability to a key");
        p.sendMessage("  §f/jjk keybind list §7— show your keybinds");
        p.sendMessage("  §f/jjk keybind clear <key> §7— remove a keybind");
        p.sendMessage("  §7Supported keys: §f" + String.join(", ", KeybindManager.SUPPORTED_KEYS));
        p.sendMessage("  §7Example: §f/jjk keybind blue F");
        p.sendMessage("  §7Hold key 2+ seconds for Maximum Output version.");
    }

    private List<String> prefixFilter(List<String> list, String input) {
        if (input == null || input.isEmpty()) return list;
        List<String> out = new ArrayList<>();
        String lower = input.toLowerCase(Locale.ROOT);
        for (String s : list) {
            if (s.toLowerCase(Locale.ROOT).startsWith(lower)) out.add(s);
        }
        return out;
    }
}
