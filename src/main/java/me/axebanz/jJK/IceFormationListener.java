package me.axebanz.jJK;

import org.bukkit.event.Listener;

public final class IceFormationListener implements Listener {

    private final JJKCursedToolsPlugin plugin;
    private final IceFormationManager mgr;

    public IceFormationListener(JJKCursedToolsPlugin plugin, IceFormationManager mgr) {
        this.plugin = plugin;
        this.mgr = mgr;
    }
    // Placeholder listener - ice block cleanup is handled internally by IceFormationManager's tasks.
}
