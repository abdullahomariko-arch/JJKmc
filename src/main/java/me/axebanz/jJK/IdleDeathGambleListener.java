package me.axebanz.jJK;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public final class IdleDeathGambleListener implements Listener {

    private final JJKCursedToolsPlugin plugin;
    private final IdleDeathGambleManager idgManager;

    public IdleDeathGambleListener(JJKCursedToolsPlugin plugin, IdleDeathGambleManager idgManager) {
        this.plugin = plugin;
        this.idgManager = idgManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        ItemStack item = e.getItem();
        if (item == null || item.getType() != Material.LEVER) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;

        // Check if it's our Pachinko Lever
        Component name = meta.displayName();
        if (name == null) return;
        // Use plain text comparison
        String plain = PlainTextComponentSerializer.plainText().serialize(name);
        if (!plain.contains("Pachinko Lever")) return;

        e.setCancelled(true);

        org.bukkit.event.block.Action action = e.getAction();
        if (action == org.bukkit.event.block.Action.RIGHT_CLICK_AIR
                || action == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            idgManager.spin(p);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;

        if (idgManager.isJackpotActive(p.getUniqueId())) {
            // Cancel lethal damage during jackpot
            if (p.getHealth() - e.getFinalDamage() <= 0) {
                e.setCancelled(true);
                p.setHealth(p.getMaxHealth());
                p.sendMessage(plugin.cfg().prefix() + "§6★ JACKPOT §7— Auto-heal saved you!");
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player p = e.getPlayer();
        idgManager.cleanup(p.getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        idgManager.cleanup(e.getPlayer().getUniqueId());
    }
}
