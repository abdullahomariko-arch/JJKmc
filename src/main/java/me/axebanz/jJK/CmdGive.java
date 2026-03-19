package me.axebanz.jJK;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public final class CmdGive implements SubCommand {

    private final JJKCursedToolsPlugin plugin;
    public CmdGive(JJKCursedToolsPlugin plugin) { this.plugin = plugin; }

    @Override public String name() { return "give"; }
    @Override public String permission() { return "jjk.give"; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        String pref = plugin.cfg().prefix();

        if (args.length < 3) {
            sender.sendMessage(pref + "§cUsage: /jjk give <player> <tool> [amount]");
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(pref + "§cPlayer not found.");
            return true;
        }

        ToolId tool = ToolId.from(args[2]);
        if (tool == null) {
            if (args[2].equalsIgnoreCase("bindingvow") || args[2].equalsIgnoreCase("binding_vow")) {
                int amount = 1;
                if (args.length >= 4) {
                    try { amount = Math.max(1, Integer.parseInt(args[3])); } catch (Exception ignored) {}
                }
                if (plugin.seanceManager() == null) {
                    sender.sendMessage(pref + "§cSeance system is not loaded.");
                    return true;
                }
                for (int i = 0; i < amount; i++) {
                    target.getInventory().addItem(plugin.seanceManager().createBindingVowItem());
                }
                sender.sendMessage(pref + "§aGiven §fBinding Vow §ax" + amount + " to §f" + target.getName() + "§a.");
                return true;
            }
            // FIXED: Show all available items including straw doll items
            sender.sendMessage(pref + "§cInvalid tool. Options: dragon_bone, split_soul_katana, kamutoke, inverted_spear, divine_wheel, playfulcloud, yuta_ring, imbued_cursed_katana, cursed_body, straw_doll_hammer, straw_doll_nail, judges_hammer, bindingvow");
            return true;
        }

        int amount = 1;
        if (args.length >= 4) {
            try { amount = Math.max(1, Integer.parseInt(args[3])); } catch (Exception ignored) {}
        }

        ItemStack item = plugin.tools().create(tool, amount);
        target.getInventory().addItem(item);

        sender.sendMessage(pref + "§aGiven §f" + tool.id + " §ax" + amount + " to §f" + target.getName() + "§a.");
        return true;
    }

    @Override
    public List<String> tab(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
        if (args.length == 2) {
            // FIXED: All items listed including straw doll items
            return List.of(
                    "dragon_bone",
                    "split_soul_katana",
                    "kamutoke",
                    "inverted_spear",
                    "divine_wheel",
                    "playfulcloud",
                    "yuta_ring",
                    "imbued_cursed_katana",
                    "cursed_body",
                    "straw_doll_hammer",
                    "straw_doll_nail",
                    "judges_hammer",
                    "bindingvow"
            );
        }
        return List.of();
    }
}