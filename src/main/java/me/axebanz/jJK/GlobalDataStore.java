package me.axebanz.jJK;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GlobalDataStore {
    private static GlobalDataStore instance;
    private final Map<String, Object> data = new HashMap<>();

    public static GlobalDataStore getInstance() {
        if (instance == null) instance = new GlobalDataStore();
        return instance;
    }

    public void set(String key, Object value) {
        data.put(key, value);
    }

    public Object get(String key) {
        return data.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        Object val = data.get(key);
        if (val == null) return null;
        return type.cast(val);
    }

    public boolean has(String key) {
        return data.containsKey(key);
    }

    public void remove(String key) {
        data.remove(key);
    }

    public void clear() {
        data.clear();
    }
}
