package me.axebanz.jJK;

import org.bukkit.entity.Player;

public final class IdleDeathGambleTechnique implements Technique {

    private final JJKCursedToolsPlugin plugin;

    public IdleDeathGambleTechnique(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public String id() { return "idle_death_gamble"; }
    @Override public String displayName() { return "§6§lIdle Death Gamble"; }
    @Override public String hexColor() { return "#FFD700"; }
    @Override public String glyphTag() { return "<glyph:technique_idg:colorable>"; }
    @Override public String iconColor() { return "§6"; }

    @Override
    public boolean canUse(Player p) { return true; }

    @Override
    public void castAbility(Player player, AbilitySlot slot) {
        if (slot == AbilitySlot.ONE) {
            expandDomain(player);
        } else {
            player.sendMessage(plugin.cfg().prefix() + "§6IDG: §7Slot 1 = Open Domain");
        }
    }

    private void expandDomain(Player player) {
        if (plugin.idgManager() == null || plugin.domainManager() == null) return;

        if (plugin.cooldowns().isOnCooldown(player.getUniqueId(), "idg.expand")) {
            long rem = plugin.cooldowns().remainingSeconds(player.getUniqueId(), "idg.expand");
            player.sendMessage(plugin.cfg().prefix() + "§cIDG Domain on cooldown: §f" + TimeFmt.mmss(rem));
            return;
        }

        if (plugin.domainManager().getDomain(player) != null) {
            player.sendMessage(plugin.cfg().prefix() + "§cYou already have an active domain!");
            return;
        }

        IdleDeathGambleDomain domain = new IdleDeathGambleDomain(plugin, player, plugin.idgManager());
        plugin.domainManager().expand(player, domain);
        plugin.idgManager().startGame(player);

        plugin.cooldowns().setCooldown(player.getUniqueId(), "idg.expand", 30);
    }
}