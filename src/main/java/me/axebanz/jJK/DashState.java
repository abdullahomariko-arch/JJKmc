package me.axebanz.jJK;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DashState {

    private static final Map<UUID, DashInfo> dash = new ConcurrentHashMap<>();

    private DashState() {}

    public static void set(UUID uuid, long untilMs, double bonusHearts) {
        dash.put(uuid, new DashInfo(untilMs, bonusHearts));
    }

    public static DashInfo get(UUID uuid) {
        return dash.get(uuid);
    }

    public static void clear(UUID uuid) {
        dash.remove(uuid);
    }

    public static final class DashInfo {
        public final long untilMs;
        public final double bonusHearts;

        DashInfo(long untilMs, double bonusHearts) {
            this.untilMs = untilMs;
            this.bonusHearts = bonusHearts;
        }
    }
}
