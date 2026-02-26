package me.axebanz.jJK;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandRouter implements CommandExecutor, TabCompleter {
    private final Map<String, CommandExecutor> routes = new HashMap<>();
    private final Map<String, TabCompleter> tabRoutes = new HashMap<>();

    public void register(String subcommand, CommandExecutor executor) {
        routes.put(subcommand.toLowerCase(), executor);
        if (executor instanceof TabCompleter) {
            tabRoutes.put(subcommand.toLowerCase(), (TabCompleter) executor);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§cUsage: /" + label + " <subcommand>");
            return true;
        }
        String sub = args[0].toLowerCase();
        CommandExecutor exec = routes.get(sub);
        if (exec == null) {
            sender.sendMessage("§cUnknown subcommand: " + sub);
            return true;
        }
        String[] newArgs = new String[args.length - 1];
        System.arraycopy(args, 1, newArgs, 0, newArgs.length);
        return exec.onCommand(sender, cmd, label + " " + sub, newArgs);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length <= 1) return List.copyOf(routes.keySet());
        String sub = args[0].toLowerCase();
        TabCompleter completer = tabRoutes.get(sub);
        if (completer == null) return List.of();
        String[] newArgs = new String[args.length - 1];
        System.arraycopy(args, 1, newArgs, 0, newArgs.length);
        return completer.onTabComplete(sender, cmd, alias, newArgs);
    }
}
