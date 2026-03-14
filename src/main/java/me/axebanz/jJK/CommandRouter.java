package me.axebanz.jJK;

import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;

public final class CommandRouter implements CommandExecutor, TabCompleter {

    private final JJKCursedToolsPlugin plugin;
    private final Map<String, SubCommand> subs = new HashMap<>();

    public CommandRouter(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    public void register(SubCommand cmd) {
        subs.put(cmd.name().toLowerCase(Locale.ROOT), cmd);
    }

    public void registerDefaults() {
        register(new CmdGive(plugin));
        register(new CmdReload(plugin));
        register(new CmdStatus(plugin));
        register(new CmdTechnique(plugin));
        register(new CmdHelp(plugin));
        register(new CmdPermadeath(plugin));
        if (plugin.seanceManager() != null) {
            register(new CmdSetWaitingRoom(plugin, plugin.seanceManager()));
        }
        register(new CmdRemoveBindingVow(plugin));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) return subs.get("help").execute(sender, args);

        SubCommand sub = subs.get(args[0].toLowerCase(Locale.ROOT));
        if (sub == null) return subs.get("help").execute(sender, args);

        if (sub.permission() != null && !sub.permission().isEmpty() && !sender.hasPermission(sub.permission())) {
            sender.sendMessage(plugin.cfg().prefix() + "§cNo permission.");
            return true;
        }

        return sub.execute(sender, args);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 0) return List.of();

        if (args.length == 1) {
            List<String> out = new ArrayList<>();
            for (String k : subs.keySet()) {
                SubCommand sc = subs.get(k);
                if (sc.permission() == null || sc.permission().isEmpty() || sender.hasPermission(sc.permission())) {
                    out.add(k);
                }
            }
            out.sort(String::compareToIgnoreCase);
            return prefix(out, args[0]);
        }

        SubCommand sub = subs.get(args[0].toLowerCase(Locale.ROOT));
        if (sub == null) return List.of();
        if (sub.permission() != null && !sub.permission().isEmpty() && !sender.hasPermission(sub.permission())) return List.of();

        String[] shifted = Arrays.copyOfRange(args, 1, args.length);
        return sub.tab(sender, shifted);
    }

    private List<String> prefix(List<String> list, String start) {
        if (start == null || start.isEmpty()) return list;
        List<String> out = new ArrayList<>();
        for (String s : list) if (s.toLowerCase().startsWith(start.toLowerCase())) out.add(s);
        return out;
    }
}
