package me.axebanz.jJK;

import org.bukkit.entity.Player;

public class CopyCTService {
    private final JJKCursedToolsPlugin plugin;

    public CopyCTService(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    public void copyTechnique(Player copier, Player target) {
        String targetTech = plugin.techniqueManager().getAssignedId(target.getUniqueId());
        if (targetTech == null) {
            copier.sendMessage(plugin.cfg().prefix() + "§cTarget has no technique to copy.");
            return;
        }
        copier.sendMessage(plugin.cfg().prefix() + "§aCopied technique: §e" + targetTech);
    }
}
