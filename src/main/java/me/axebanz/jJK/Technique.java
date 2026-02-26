package me.axebanz.jJK;

import org.bukkit.entity.Player;

public interface Technique {
    String getId();
    String getDisplayName();
    void castAbility(Player player, String ability);
    void onEquip(Player player);
    void onUnequip(Player player);
}
