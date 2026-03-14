package me.axebanz.jJK;

public final class HexColor {
    private HexColor() {}

    // Minimal: return a legacy-ish color close to the vibe.
    public static String legacyFromHex(String hex) {
        if (hex == null) return "§7";
        String h = hex.toUpperCase();
        if (h.contains("8A2BE2")) return "§5"; // purple vibe
        if (h.contains("00FFFF") || h.contains("00BFFF") || h.contains("1E90FF")) return "§b";
        if (h.contains("FFD700") || h.contains("FFA500")) return "§6";
        return "§d";
    }
}
