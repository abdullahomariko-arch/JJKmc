package me.axebanz.jJK;

public enum ToolId {
    DRAGON_BONE("dragon_bone"),
    SPLIT_SOUL_KATANA("split_soul_katana"),
    KAMUTOKE("kamutoke"),
    INVERTED_SPEAR("inverted_spear"),

    DIVINE_WHEEL("divine_wheel"),
    PLAYFULCLOUD("playfulcloud"),

    YUTA_RING("yuta_ring"),

    IMBUED_CURSED_KATANA("imbued_cursed_katana"),

    CURSED_BODY("cursed_body"),

    STRAW_DOLL_HAMMER("hammer"),
    STRAW_DOLL_NAIL("nail");

    public final String id;
    ToolId(String id) { this.id = id; }

    public static ToolId from(String s) {
        if (s == null) return null;
        for (ToolId t : values()) if (t.id.equalsIgnoreCase(s)) return t;
        if (s.equalsIgnoreCase("straw_doll_hammer")) return STRAW_DOLL_HAMMER;
        if (s.equalsIgnoreCase("straw_doll_nail")) return STRAW_DOLL_NAIL;
        return null;
    }
}