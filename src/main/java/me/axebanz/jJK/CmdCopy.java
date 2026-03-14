package me.axebanz.jJK;

import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

public final class CmdCopy implements CommandExecutor, TabCompleter {

    private final JJKCursedToolsPlugin plugin;
    private final RikaManager rika;
    private final CopyCTService copyCT;

    public CmdCopy(JJKCursedToolsPlugin plugin, RikaManager rika, CopyCTService copyCT) {
        this.plugin = plugin;
        this.rika = rika;
        this.copyCT = copyCT;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(plugin.cfg().prefix() + "§cPlayers only.");
            return true;
        }

        if (!plugin.techniqueManager().canUseTechniqueActions(p, true)) return true;

        if (!plugin.copy().canUseCopy(p)) {
            p.sendMessage(plugin.cfg().prefix() + "§cYou must have §dCopy§c equipped to use /copy.");
            return true;
        }

        String sub = (args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT));

        switch (sub) {
            case "help" -> {
                p.sendMessage(plugin.cfg().prefix() + "§dCopy:");
                p.sendMessage("§f/" + label + " summon §7— Summon Rika");
                p.sendMessage("§f/" + label + " dismiss §7— Dismiss Rika");
                p.sendMessage("§f/" + label + " beam §7— Fire Rika's beam");
                p.sendMessage("§f/" + label + " storage §7— Open Rika's storage");
                p.sendMessage("§f/" + label + " ct §7— Use copied technique");
                p.sendMessage("§f/" + label + " shuffle §7— Switch between copied techniques");
                p.sendMessage("§f/" + label + " return §7— Recall Rika to your side");
                return true;
            }
            case "summon" -> { rika.trySummon(p); return true; }
            case "dismiss" -> { rika.dismiss(p); return true; }
            case "beam" -> { rika.tryLoveBeam(p); return true; }
            case "storage" -> { plugin.rikaStorage().open(p); return true; }
            case "ct" -> { copyCT.tryUseCopiedTechnique(p); return true; }
            case "shuffle" -> { copyCT.shuffleCopiedTechnique(p); return true; }
            case "return" -> { rika.returnToOwner(p); return true; }
            default -> {
                p.sendMessage(plugin.cfg().prefix() + "§cUsage: /" + label + " <summon|dismiss|beam|storage|ct|shuffle|return>");
                return true;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!(sender instanceof Player p)) return List.of();
        if (!plugin.copy().canUseCopy(p)) return List.of();

        if (args.length == 1) return List.of("summon", "dismiss", "beam", "storage", "ct", "shuffle", "return");
        return List.of();
    }
}