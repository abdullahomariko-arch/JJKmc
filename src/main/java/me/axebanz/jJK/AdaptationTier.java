package me.axebanz.jJK;

public enum AdaptationTier {
    RECOGNITION(0, "§7Recognition", 0.0, "The phenomenon is registered.", 0),
    RESISTANCE(1, "§eResistance", 0.20, "Damage reduced by 20%.", 100),
    CORRECTION(2, "§6Correction", 0.40, "Damage reduced by 40%.", 200),
    NEGATION(3, "§cNegation", 0.60, "Damage reduced by 60%.", 300),
    COUNTER_INTEGRATION(4, "§5Counter-Integration", 0.80, "Damage reduced by 80%.", 400);

    private final int level;
    private final String displayName;
    private final double damageReduction;
    private final String description;
    private final int totalHitsRequired;

    AdaptationTier(int level, String displayName, double damageReduction, String description, int totalHitsRequired) {
        this.level = level;
        this.displayName = displayName;
        this.damageReduction = damageReduction;
        this.description = description;
        this.totalHitsRequired = totalHitsRequired;
    }

    public int level() { return level; }
    public String displayName() { return displayName; }
    public double damageReduction() { return damageReduction; }
    public String description() { return description; }
    public int totalHitsRequired() { return totalHitsRequired; }

    public static AdaptationTier fromLevel(int level) {
        for (AdaptationTier t : values()) {
            if (t.level == level) return t;
        }
        return RECOGNITION;
    }

    public AdaptationTier next() {
        if (this == COUNTER_INTEGRATION) return COUNTER_INTEGRATION;
        return fromLevel(this.level + 1);
    }
}