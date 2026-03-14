package me.axebanz.jJK;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

public final class CmdIdleDeathGamble implements CommandExecutor, TabCompleter {

    private final JJKCursedToolsPlugin plugin;

    public CmdIdleDeathGamble(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(plugin.cfg().prefix() + "§cPlayers only.");
            return true;
        }

        if (plugin.idgManager() == null) {
            p.sendMessage(plugin.cfg().prefix() + "§cIDG system not loaded.");
            return true;
        }

        String sub = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "expand" -> {
                Technique tech = plugin.techniqueRegistry().get("idle_death_gamble");
                if (tech != null) tech.castAbility(p, AbilitySlot.ONE);
                else p.sendMessage(plugin.cfg().prefix() + "§cIdle Death Gamble technique not registered.");
                return true;
            }
            case "spin" -> {
                plugin.idgManager().spin(p);
                return true;
            }
            case "status" -> {
                IdleDeathGambleManager.IDGGameState state = plugin.idgManager().getState(p.getUniqueId());
                if (state == null) {
                    p.sendMessage(plugin.cfg().prefix() + "§7No IDG game active.");
                    return true;
                }
                if (state.jackpotActive) {
                    long rem = plugin.idgManager().getJackpotTimeRemaining(p.getUniqueId());
                    long secs = rem / 20;
                    p.sendMessage(plugin.cfg().prefix() + "§6★ JACKPOT ACTIVE §8| §fTime: §e" + TimeFmt.mmss(secs));
                } else if (state.domainActive) {
                    p.sendMessage(plugin.cfg().prefix() + "§6IDG Domain §8| §fSpins: §e" + state.spinCount + "§f/" + IdleDeathGambleManager.MAX_SPINS
                            + " §8| §aG:" + state.greenIndicators + " §cR:" + state.redIndicators + " §6Au:" + state.goldIndicators);
                } else {
                    p.sendMessage(plugin.cfg().prefix() + "§7IDG inactive.");
                }
                return true;
            }
            default -> {
                p.sendMessage(plugin.cfg().prefix() + "§6Idle Death Gamble:");
                p.sendMessage("§f/" + label + " expand §7— Open domain");
                p.sendMessage("§f/" + label + " spin §7— Spin the pachinko");
                p.sendMessage("§f/" + label + " status §7— Show game state");
                return true;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) return List.of("expand", "spin", "status");
        return List.of();
    }
}
