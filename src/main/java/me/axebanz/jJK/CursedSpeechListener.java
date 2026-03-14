package me.axebanz.jJK;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;
import org.bukkit.util.Vector;

public final class CursedSpeechListener implements Listener {

    private final JJKCursedToolsPlugin plugin;
    private final CursedSpeechManager cursedSpeech;

    public CursedSpeechListener(JJKCursedToolsPlugin plugin, CursedSpeechManager cursedSpeech) {
        this.plugin = plugin;
        this.cursedSpeech = cursedSpeech;
    }

    // Detect drinking WATER bottle
    @EventHandler
    public void onConsume(PlayerItemConsumeEvent e) {
        Player p = e.getPlayer();
        ItemStack it = e.getItem();
        if (it == null) return;

        if (it.getType() == Material.POTION && it.getItemMeta() instanceof PotionMeta meta) {
            if (meta.getBasePotionType() == PotionType.WATER) {
                cursedSpeech.onDrinkWater(p);
            }
        }
    }

    // Hard movement lock: cancel velocity changes from movement inputs
    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (!cursedSpeech.isNoMove(p.getUniqueId())) return;

        // allow head rotation but prevent position change
        if (e.getFrom().getX() != e.getTo().getX()
                || e.getFrom().getY() != e.getTo().getY()
                || e.getFrom().getZ() != e.getTo().getZ()) {
            e.setTo(e.getFrom());
            p.setVelocity(new Vector(0, 0, 0));
        }
    }

    // Block ender pearls / chorus fruit usage while NoMove
    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!cursedSpeech.isNoMove(p.getUniqueId())) return;

        ItemStack it = e.getItem();
        if (it == null) return;

        if (it.getType() == Material.ENDER_PEARL || it.getType() == Material.CHORUS_FRUIT) {
            e.setCancelled(true);
            p.sendMessage(plugin.cfg().prefix() + "§cDON'T MOVE!");
        }
    }

    // Block teleports while NoMove (covers pearls/chorus/commands/plugins)
    @EventHandler
    public void onTeleport(PlayerTeleportEvent e) {
        Player p = e.getPlayer();
        if (!cursedSpeech.isNoMove(p.getUniqueId())) return;

        e.setCancelled(true);
        p.sendMessage(plugin.cfg().prefix() + "§cDON'T MOVE!");
    }

    // Block general teleports just in case (some APIs use EntityTeleportEvent)
    @EventHandler
    public void onEntityTeleport(EntityTeleportEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (!cursedSpeech.isNoMove(p.getUniqueId())) return;

        e.setCancelled(true);
        p.sendMessage(plugin.cfg().prefix() + "§cDON'T MOVE!");
    }
}