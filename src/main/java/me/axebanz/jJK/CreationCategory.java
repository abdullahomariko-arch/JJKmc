package me.axebanz.jJK;

public enum CreationCategory {
    BLOCKS("Blocks", "§9", 3, 64),
    TOOLS_ARMOUR("Tools & Armour", "§6", 5, 3),
    CONSUMABLES("Consumables", "§a", 7, 64);

    private final String displayName;
    private final String color;
    private final int ceCost;
    private final int amountGiven;

    CreationCategory(String displayName, String color, int ceCost, int amountGiven) {
        this.displayName = displayName;
        this.color = color;
        this.ceCost = ceCost;
        this.amountGiven = amountGiven;
    }

    public String displayName() { return displayName; }
    public String color() { return color; }
    public int ceCost() { return ceCost; }
    public int amountGiven() { return amountGiven; }

    public CreationCategory next() {
        CreationCategory[] vals = values();
        for (int i = 0; i < vals.length; i++) {
            if (vals[i] == this) {
                return vals[(i + 1) % vals.length];
            }
        }
        return BLOCKS;
    }

    public static CreationCategory from(String s) {
        if (s == null) return BLOCKS;
        try { return CreationCategory.valueOf(s.toUpperCase()); }
        catch (Exception ignored) { return BLOCKS; }
    }
}