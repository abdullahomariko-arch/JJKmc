package me.axebanz.jJK;

import org.bukkit.entity.Player;

public final class ContractualContractsTechnique implements Technique {
    @Override public String id() { return "contractual_contracts"; }
    @Override public String displayName() { return "§5Contractual Contracts"; }
    @Override public String hexColor() { return "#9400D3"; }
    @Override
    public void castAbility(Player player, AbilitySlot slot) {
        player.sendMessage("§5Contractual Contracts §7— coming soon.");
    }
}
