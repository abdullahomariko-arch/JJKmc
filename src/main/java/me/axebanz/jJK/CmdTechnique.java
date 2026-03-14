package me.axebanz.jJK;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class CmdTechnique implements SubCommand {

    private final JJKCursedToolsPlugin plugin;
    public CmdTechnique(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public String name() { return "technique"; }

    // Keep permission as-is; players casting is handled by the command router permission check.
    @Override public String permission() { return "jjk.technique"; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        String pref = plugin.cfg().prefix();

        // PLAYER CAST: /jjk technique 1|2|3
        if (sender instanceof Player player && args.length == 2) {
            try {
                int num = Integer.parseInt(args[1]);
                AbilitySlot slot = AbilitySlot.fromInt(num);
                if (slot == null) return true;

                plugin.techniqueManager().cast(player, slot);
                return true;
            } catch (NumberFormatException ignored) {
                // fallthrough to help
            }
        }

        // ADMIN: /jjk technique set <player> <technique>
        if (args.length < 2 || !args[1].equalsIgnoreCase("set")) {
            sender.sendMessage(pref + "§cUsage:");
            sender.sendMessage(pref + "§f/jjk technique <1|2|3>");
            sender.sendMessage(pref + "§f/jjk technique set <player> <technique>");
            return true;
        }

        if (args.length < 4) {
            sender.sendMessage(pref + "§cUsage: /jjk technique set <player> <technique>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[2]);
        if (target == null) {
            sender.sendMessage(pref + "§cPlayer not found.");
            return true;
        }

        String techId = args[3];
        if (plugin.techniques().get(techId) == null) {
            sender.sendMessage(pref + "§cUnknown technique. Available: " +
                    String.join(", ", plugin.techniques().ids()));
            return true;
        }

        plugin.techniqueManager().forceSetTechnique(target.getUniqueId(), techId);
        sender.sendMessage(pref + "§aSet technique of §f" + target.getName() + " §ato §f" + techId + "§a.");
        return true;
    }

    @Override
    public List<String> tab(CommandSender sender, String[] args) {
        // args are shifted by CommandRouter (it passes args after "technique")
        if (args.length == 1) {
            // Suggest set + ability slots for convenience
            return List.of("set", "1", "2", "3");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            return new ArrayList<>(plugin.techniques().ids()); // <— all techniques available
        }
        return List.of();
    }
}