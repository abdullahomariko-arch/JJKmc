package me.axebanz.jJK;

public class TimeFmt {
    public static String format(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        if (minutes > 0) {
            return minutes + "m" + seconds + "s";
        }
        return seconds + "s";
    }

    public static String formatTicks(int ticks) {
        return format((long) ticks * 50L);
    }
}
