package me.axebanz.jJK;

public enum ShikigamiType {
    DIVINE_DOGS("divine_dogs", "§fDivine Dogs", 80, true, false, "divine_dogs"),
    DIVINE_DOG_TOTALITY("divine_dog_totality", "§8Divine Dog: Totality", 160, false, false, "divine_dog_totality"),
    TOAD("toad", "§aToad", -1, true, true, "toad"),
    RABBIT_ESCAPE("rabbit_escape", "§fRabbit Escape", -1, false, true, null),
    NUE("nue", "§eNue", -1, true, true, "nue"),
    GREAT_SERPENT("great_serpent", "§2Great Serpent", 140, true, true, "great_serpent"),
    MAX_ELEPHANT("max_elephant", "§bMax Elephant", 200, true, true, "max_elephant"),
    NUE_TOTALITY("nue_totality", "§6Nue: Totality", 480, false, true, "nue_totality"),
    ROUND_DEER("round_deer", "§aRound Deer", 160, true, true, "round_deer"),
    PIERCING_OX("piercing_ox", "§4Piercing Ox", 180, true, true, "piercing_ox"),
    TIGER_FUNERAL("tiger_funeral", "§6Tiger Funeral", 170, true, true, "tiger_funeral"),
    MAHORAGA("mahoraga", "§5§lEight-Handled Sword Divergent Sila Divine General Mahoraga", 1000, false, true, "mahoraga");

    private final String id;
    private final String displayName;
    private final double maxHealth;
    private final boolean respawns;
    private final boolean requiresRitual;
    private final String modelId;

    ShikigamiType(String id, String displayName, double maxHealth, boolean respawns, boolean requiresRitual, String modelId) {
        this.id = id;
        this.displayName = displayName;
        this.maxHealth = maxHealth;
        this.respawns = respawns;
        this.requiresRitual = requiresRitual;
        this.modelId = modelId;
    }

    public String id() { return id; }
    public String displayName() { return displayName; }
    public double maxHealth() { return maxHealth; }
    public boolean respawns() { return respawns; }
    public boolean requiresRitual() { return requiresRitual; }
    public String modelId() { return modelId; }

    public boolean isIndestructible() {
        return maxHealth < 0;
    }

    /** Whether this shikigami uses ArmorStand + ModelEngine (like Rika) instead of a real mob */
    public boolean usesArmorStandModel() {
        return false; // Mahoraga now uses Iron Golem for proper pathfinding/hitbox
    }

    /** Whether this shikigami uses Iron Golem as base entity with ModelEngine overlay */
    public boolean usesIronGolemModel() {
        return this == MAHORAGA;
    }

    /** Whether this shikigami spawns as a swarm of entities */
    public boolean isSwarm() {
        return this == RABBIT_ESCAPE;
    }

    /** Whether this shikigami spawns as a pair */
    public boolean isPair() {
        return this == DIVINE_DOGS;
    }

    /**
     * Returns the fusion result when this shikigami is permanently destroyed.
     * Returns null if no fusion exists.
     */
    public ShikigamiType fusionResult() {
        return switch (this) {
            case DIVINE_DOGS -> DIVINE_DOG_TOTALITY;
            case NUE, GREAT_SERPENT -> NUE_TOTALITY;
            default -> null;
        };
    }

    public ShikigamiType[] fusionSources() {
        return switch (this) {
            case DIVINE_DOG_TOTALITY -> new ShikigamiType[]{DIVINE_DOGS};
            case NUE_TOTALITY -> new ShikigamiType[]{NUE, GREAT_SERPENT};
            default -> new ShikigamiType[]{};
        };
    }

    /** @deprecated Toad Totality does not exist */
    @Deprecated
    public static boolean isToadTotalityAvailable(TenShadowsProfile prof) {
        return false;
    }

    public static ShikigamiType from(String s) {
        if (s == null) return null;
        for (ShikigamiType t : values()) {
            if (t.id.equalsIgnoreCase(s)) return t;
        }
        try { return ShikigamiType.valueOf(s.toUpperCase()); }
        catch (Exception ignored) { return null; }
    }
}