package me.axebanz.jJK;

public enum AbilitySlot {
    ONE(1),
    TWO(2),
    THREE(3),
    FOUR(4),
    FIVE(5);

    private final int number;

    AbilitySlot(int number) {
        this.number = number;
    }

    public int number() {
        return number;
    }

    public static AbilitySlot fromInt(int i) {
        return switch (i) {
            case 1 -> ONE;
            case 2 -> TWO;
            case 3 -> THREE;
            case 4 -> FOUR;
            case 5 -> FIVE;
            default -> null;
        };
    }
}
