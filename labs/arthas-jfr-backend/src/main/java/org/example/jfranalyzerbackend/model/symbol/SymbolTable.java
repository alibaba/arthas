package org.example.jfranalyzerbackend.model.symbol;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SymbolTable<T> {
    private final Map<T, T> table = new ConcurrentHashMap<>();

    public boolean isContains(T s) {
        return table.containsKey(s);
    }

    public T get(T s) {
        return table.get(s);
    }

    public T put(T s) {
        return table.put(s, s);
    }

    public void clear() {
        this.table.clear();
    }
}
