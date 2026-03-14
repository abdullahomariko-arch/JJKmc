package me.axebanz.jJK;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public final class WheelDamageListener implements Listener {

    private final WheelCombatHandler wheelCombat;

    public WheelDamageListener(WheelCombatHandler wheelCombat) {
        this.wheelCombat = wheelCombat;
    }

    @EventHandler
    public void onAnyDamage(EntityDamageEvent e) {
        if (e.isCancelled()) return;
        if (!(e.getEntity() instanceof Player victim)) return;

        // Fire/lava/hot-floor are environmental => handle here
        switch (e.getCause()) {
            case FIRE, FIRE_TICK, LAVA, HOT_FLOOR -> wheelCombat.handleWheelDefense(e, victim);
            default -> {}
        }
    }
}