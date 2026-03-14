package me.axebanz.jJK;

public enum AdaptationCategory {
    MELEE("Melee", "§c", 100),
    PROJECTILE("Projectile", "§9", 100),
    EXPLOSION("Explosion", "§6", 100),
    FIRE("Fire", "§e", 100),
    LIGHTNING("Lightning", "§b", 100),
    TECHNIQUE("Technique", "§5", 100),
    TRUE_DAMAGE("True Damage", "§f", 100);

    private final String displayName;
    private final String color;
    private final int hitsToMax;

    AdaptationCategory(String displayName, String color, int hitsToMax) {
        this.displayName = displayName;
        this.color = color;
        this.hitsToMax = hitsToMax;
    }

    public String displayName() { return displayName; }
    public String color() { return color; }
    public int hitsToMax() { return hitsToMax; }

    public AdaptationCategory next() {
        AdaptationCategory[] vals = values();
        for (int i = 0; i < vals.length; i++) {
            if (vals[i] == this) {
                return vals[(i + 1) % vals.length];
            }
        }
        return MELEE;
    }

    public static AdaptationCategory from(String s) {
        if (s == null) return MELEE;
        try { return AdaptationCategory.valueOf(s.toUpperCase()); }
        catch (Exception ignored) { return MELEE; }
    }
}