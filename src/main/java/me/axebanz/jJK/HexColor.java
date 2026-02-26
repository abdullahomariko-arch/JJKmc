package me.axebanz.jJK;

public class HexColor {
    public static String fromHex(String hex) {
        if (hex == null || hex.isEmpty()) return "";
        // Strip '#' if present
        if (hex.startsWith("#")) hex = hex.substring(1);
        try {
            int r = Integer.parseInt(hex.substring(0, 2), 16);
            int g = Integer.parseInt(hex.substring(2, 4), 16);
            int b = Integer.parseInt(hex.substring(4, 6), 16);
            return net.md_5.bungee.api.ChatColor.of(new java.awt.Color(r, g, b)).toString();
        } catch (Exception e) {
            return "";
        }
    }
}
