package com.houtong.repository;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MemoryDatabase {

    private final Map<String, Object> database = new ConcurrentHashMap<>();

    public Object get(String key) {
        return database.get(key);
    }

    public void put(String key, Object value) {
        database.put(key, value);
    }

    public Object remove(String key) {
        return database.remove(key);
    }

    public boolean containsKey(String key){
        return database.containsKey(key);
    }
}

