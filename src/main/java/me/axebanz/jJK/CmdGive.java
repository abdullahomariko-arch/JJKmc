package me.axebanz.jJK;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * /jjkgive command handler.
 * Bug Fix #10: Tab complete for args.length == 2 includes straw_doll_hammer,
 * straw_doll_nail, and cursed_body.
 */
public class CmdGive implements CommandExecutor, TabCompleter {
    private final JJKCursedToolsPlugin plugin;
    private final CursedToolFactory toolFactory;

    public CmdGive(JJKCursedToolsPlugin plugin, CursedToolFactory toolFactory) {
        this.plugin = plugin;
        this.toolFactory = toolFactory;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("jjk.admin")) {
            sender.sendMessage(plugin.cfg().prefix() + "§cNo permission.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(plugin.cfg().prefix() + "§cUsage: /" + label + " <player> <item> [amount]");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(plugin.cfg().prefix() + "§cPlayer not found: " + args[0]);
            return true;
        }

        String itemId = args[1].toLowerCase();
        int amount = 1;
        if (args.length >= 3) {
            try {
                amount = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage(plugin.cfg().prefix() + "§cInvalid amount: " + args[2]);
                return true;
            }
        }

        ItemStack item = toolFactory.createItem(itemId, amount);
        if (item == null) {
            sender.sendMessage(plugin.cfg().prefix() + "§cUnknown item: " + itemId);
            return true;
        }

        target.getInventory().addItem(item);
        sender.sendMessage(plugin.cfg().prefix() + "§aGave §e" + amount + "x " + itemId + "§a to §e" + target.getName());
        return true;
    }

    @Override
    public List<String> tab(CommandSender sender, Command cmd, String alias, String[] args) {
        return onTabComplete(sender, cmd, alias, args);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!sender.hasPermission("jjk.admin")) return List.of();

        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(prefix))
                    .collect(Collectors.toList());
        }

        // Bug Fix #10: Add straw_doll_hammer, straw_doll_nail, cursed_body to item list
        if (args.length == 2) {
            List<String> items = Arrays.asList(
                    "split_soul_katana",
                    "cursed_blade",
                    "straw_doll_hammer",
                    "straw_doll_nail",
                    "cursed_body",
                    "binding_vow_scroll",
                    "cursed_tool",
                    "domain_scroll"
            );
            String prefix = args[1].toLowerCase();
            return items.stream()
                    .filter(i -> i.startsWith(prefix))
                    .collect(Collectors.toList());
        }

        return List.of();
    }
}
