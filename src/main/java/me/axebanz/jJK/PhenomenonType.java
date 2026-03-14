package me.axebanz.jJK;

public enum PhenomenonType {
    // PHYSICAL CATEGORY
    SLASHING("Slashing", "§c", "PHYSICAL"),
    BLUNT("Blunt", "§8", "PHYSICAL"),
    PIERCING("Piercing", "§4", "PHYSICAL"),

    // ELEMENTAL CATEGORY
    THERMAL("Thermal", "§e", "ELEMENTAL"),
    ELECTRICAL("Electrical", "§b", "ELEMENTAL"),
    FREEZING("Freezing", "§3", "ELEMENTAL"),

    // SPATIAL CATEGORY
    KNOCKBACK("Knockback", "§6", "SPATIAL"),
    PULL("Pull", "§5", "SPATIAL"),
    BARRIER("Barrier", "§9", "SPATIAL"),

    // ENERGY CATEGORY
    CURSED_ENERGY("Cursed Energy", "§d", "ENERGY"),
    TRUE_DAMAGE("True Damage", "§f", "ENERGY"),
    DOMAIN("Domain", "§a", "ENERGY"),

    // TECHNIQUE CATEGORY
    CURSE_TECHNIQUE("Curse Technique", "§d", "TECHNIQUE"),
    NEGATION("Negation", "§c", "TECHNIQUE"),
    NULLIFICATION("Nullification", "§5", "TECHNIQUE");

    private final String displayName;
    private final String color;
    private final String mainCategory;

    PhenomenonType(String displayName, String color, String mainCategory) {
        this.displayName = displayName;
        this.color = color;
        this.mainCategory = mainCategory;
    }

    public String displayName() { return displayName; }
    public String color() { return color; }
    public String mainCategory() { return mainCategory; }

    public static PhenomenonType from(String s) {
        if (s == null) return SLASHING;
        try { return PhenomenonType.valueOf(s.toUpperCase()); }
        catch (Exception ignored) { return SLASHING; }
    }
}