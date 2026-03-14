package me.axebanz.jJK;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

public final class CmdPermadeath implements SubCommand {

    private final JJKCursedToolsPlugin plugin;

    public CmdPermadeath(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public String name() { return "permadeath"; }
    @Override public String permission() { return "jjk.admin"; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        String pref = plugin.cfg().prefix();

        String sub = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : "";

        switch (sub) {
            case "on" -> {
                plugin.cfg().setPermadeathEnabled(true);
                sender.sendMessage(pref + "§aPermadeath system §2ENABLED§a.");
            }
            case "off" -> {
                plugin.cfg().setPermadeathEnabled(false);
                sender.sendMessage(pref + "§cPermadeath system §4DISABLED§c.");
            }
            case "toggle" -> {
                boolean newState = !plugin.cfg().permadeathEnabled();
                plugin.cfg().setPermadeathEnabled(newState);
                sender.sendMessage(pref + "§7Permadeath toggled " + (newState ? "§aON" : "§cOFF") + "§7.");
            }
            case "reset" -> {
                if (args.length < 3) {
                    sender.sendMessage(pref + "§cUsage: /jjk permadeath reset <player>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[2]);
                if (target == null) {
                    sender.sendMessage(pref + "§cPlayer not found (must be online).");
                    return true;
                }
                PlayerProfile prof = plugin.data().get(target.getUniqueId());
                prof.permaDead = false;
                prof.permaDeadTechniqueId = null;
                prof.seanceBindingVowActive = false;
                prof.isReincarnated = false;
                prof.seanceSpawnWorld = null;
                plugin.data().save(target.getUniqueId());

                plugin.seanceManager().unbanPlayer(target.getUniqueId());

                target.setGameMode(GameMode.SURVIVAL);
                target.setHealth(target.getMaxHealth());
                target.setFoodLevel(20);

                sender.sendMessage(pref + "§aReset permadeath state for §f" + target.getName() + "§a.");
                target.sendMessage(pref + "§aYour permadeath state has been reset by an admin.");
            }
            default -> sender.sendMessage(pref + "§cUsage: /jjk permadeath <toggle|on|off|reset <player>>");
        }
        return true;
    }

    @Override
    public List<String> tab(CommandSender sender, String[] args) {
        if (args.length == 1) return List.of("toggle", "on", "off", "reset");
        if (args.length == 2 && args[0].equalsIgnoreCase("reset")) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
        return List.of();
    }
}