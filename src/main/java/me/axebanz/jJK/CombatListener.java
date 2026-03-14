package me.axebanz.jJK;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.inventory.ItemStack;

public final class CombatListener implements Listener {

    private final JJKCursedToolsPlugin plugin;
    private final AbilityService abilities;
    private final CursedToolFactory tools;
    private final RegenLockManager regenLock;
    private final NullifyManager nullify;
    private final WheelCombatHandler wheelCombat;

    public CombatListener(JJKCursedToolsPlugin plugin, AbilityService abilities, CursedToolFactory tools, RegenLockManager regenLock, NullifyManager nullify, WheelCombatHandler wheelCombat) {
        this.plugin = plugin;
        this.abilities = abilities;
        this.tools = tools;
        this.regenLock = regenLock;
        this.nullify = nullify;
        this.wheelCombat = wheelCombat;
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        if (e.isCancelled()) return;

        // Skip Rika entities entirely — CopyListener handles her invulnerability
        if (plugin.rika() != null && plugin.rika().isRikaEntity(e.getEntity())) return;

        if (e.getEntity() instanceof Player victim) {
            wheelCombat.handleWheelDefense(e, victim);
        }

        if (!(e.getDamager() instanceof Player attacker)) return;

        ItemStack it = attacker.getInventory().getItemInMainHand();
        ToolId id = tools.identify(it);

        Entity victimEnt = e.getEntity();

        DashState.DashInfo dash = DashState.get(attacker.getUniqueId());
        if (dash != null && System.currentTimeMillis() <= dash.untilMs) {
            double bonus = dash.bonusHearts * 2.0;
            e.setDamage(e.getDamage() + bonus);
            DashState.clear(attacker.getUniqueId());
        }

        // ===== Copy owner hit -> set Rika target =====
        if (victimEnt instanceof LivingEntity le && plugin.copy() != null && plugin.copy().canUseCopy(attacker)) {
            if (plugin.rika() != null && plugin.rika().isActive(attacker)) {
                // Don't target Rika entities
                if (!plugin.rika().isRikaEntity(le)) {
                    plugin.rika().setTarget(attacker, le);
                }
            }
        }

        // ✅ Split Soul Katana ON-HIT
        if (id == ToolId.SPLIT_SOUL_KATANA && victimEnt instanceof LivingEntity le) {
            abilities.handleSplitSoulHit(attacker, le);
            return;
        }

        if (id == ToolId.INVERTED_SPEAR && victimEnt instanceof Player vp) {
            abilities.handleIsohHit(attacker, vp);
        }

        // ✅ Playful Cloud ON-HIT
        if (id == ToolId.PLAYFULCLOUD && victimEnt instanceof LivingEntity le) {
            plugin.playfulCloud().onHit(attacker, le);
            return;
        }
    }

    @EventHandler
    public void onRegen(EntityRegainHealthEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (!regenLock.isLocked(p.getUniqueId())) return;
        e.setCancelled(true);
    }
}