package me.axebanz.jJK;

/**
 * Mahoraga's wheel adaptation tiers.
 * The wheel spins a maximum of 4 times with a 3-minute gap between spins.
 * Each tier increases damage reduction from the adapted phenomenon type.
 */
public enum AdaptationTier {
    /** Tier 0 — Wheel not yet spun */
    RECOGNITION(0, "§7Recognition", 0.0, "The phenomenon is registered.", 0),
    /** Tier 1 — After 1st spin (3 min), 25% reduction */
    RESISTANCE(1, "§eResistance", 0.25, "Damage reduced by 25%.", 1),
    /** Tier 2 — After 2nd spin (6 min), 50% reduction */
    CORRECTION(2, "§6Correction", 0.50, "Damage reduced by 50%.", 2),
    /** Tier 3 — After 3rd spin (9 min), 75% reduction */
    NEGATION(3, "§cNegation", 0.75, "Damage reduced by 75%.", 3),
    /** Tier 4 — After 4th spin (12 min), 90% reduction + counter-attack bonus */
    COUNTER_INTEGRATION(4, "§5Counter-Integration", 0.90, "Damage reduced by 90% + counter-attack.", 4);

    /** How long between wheel spins in milliseconds (2 minutes) */
    public static final long SPIN_GAP_MS = 2L * 60L * 1000L;
    /** Maximum wheel spins */
    public static final int MAX_SPINS = 4;

    private final int level;
    private final String displayName;
    private final double damageReduction;
    private final String description;
    private final int spinsRequired;

    AdaptationTier(int level, String displayName, double damageReduction, String description, int spinsRequired) {
        this.level = level;
        this.displayName = displayName;
        this.damageReduction = damageReduction;
        this.description = description;
        this.spinsRequired = spinsRequired;
    }

    public int level() { return level; }
    public String displayName() { return displayName; }
    public double damageReduction() { return damageReduction; }
    public String description() { return description; }
    /** Number of wheel spins required to reach this tier */
    public int spinsRequired() { return spinsRequired; }

    /** @deprecated use spinsRequired() */
    @Deprecated
    public int totalHitsRequired() { return spinsRequired * 100; }

    public static AdaptationTier fromLevel(int level) {
        for (AdaptationTier t : values()) {
            if (t.level == level) return t;
        }
        return RECOGNITION;
    }

    public static AdaptationTier fromSpins(int spins) {
        AdaptationTier result = RECOGNITION;
        for (AdaptationTier t : values()) {
            if (spins >= t.spinsRequired) result = t;
        }
        return result;
    }

    public AdaptationTier next() {
        if (this == COUNTER_INTEGRATION) return COUNTER_INTEGRATION;
        return fromLevel(this.level + 1);
    }
}