package me.axebanz.jJK;

public final class TimeFmt {
    private TimeFmt() {}

    public static String mmss(long totalSeconds) {
        long m = totalSeconds / 60;
        long s = totalSeconds % 60;
        return m + ":" + (s < 10 ? "0" + s : String.valueOf(s));
    }
}

