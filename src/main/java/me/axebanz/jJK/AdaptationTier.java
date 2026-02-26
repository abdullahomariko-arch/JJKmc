package me.axebanz.jJK;

public enum AdaptationTier {
    TIER_1(1),
    TIER_2(2),
    TIER_3(3),
    TIER_4(4),
    TIER_5(5);

    private final int level;

    AdaptationTier(int level) { this.level = level; }
    public int getLevel() { return level; }
}
