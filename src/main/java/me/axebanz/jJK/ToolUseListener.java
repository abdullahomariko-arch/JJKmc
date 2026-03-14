package me.axebanz.jJK;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public final class ToolUseListener implements Listener {

    private final JJKCursedToolsPlugin plugin;
    private final AbilityService abilities;
    private final CursedToolFactory tools;

    public ToolUseListener(JJKCursedToolsPlugin plugin, AbilityService abilities, CursedToolFactory tools) {
        this.plugin = plugin;
        this.abilities = abilities;
        this.tools = tools;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player p = e.getPlayer();
        ItemStack it = p.getInventory().getItemInMainHand();

        ToolId id = tools.identify(it);
        if (id == null) return;

        // Let the Straw Doll Hammer (BOW) pass through so it can draw/shoot normally.
        // StrawDollListener handles resonance/hairpin and only cancels when those activate.
        if (id == ToolId.STRAW_DOLL_HAMMER) return;

        abilities.tryUseAbility(p, id, it);

        e.setCancelled(true);
    }
}