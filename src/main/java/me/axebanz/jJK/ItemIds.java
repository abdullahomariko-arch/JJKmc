package me.axebanz.jJK;

import org.bukkit.NamespacedKey;

public final class ItemIds {

    public final NamespacedKey TOOL_ID;
    public final NamespacedKey TOOL_VERSION;

    public ItemIds(JJKCursedToolsPlugin plugin) {
        TOOL_ID = new NamespacedKey(plugin, "tool_id");
        TOOL_VERSION = new NamespacedKey(plugin, "tool_version");
    }
}
