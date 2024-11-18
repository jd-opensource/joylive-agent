package com.jd.live.agent.core.config;

import java.util.*;

/**
 * Config watcher manager
 */
public class ConfigWatcherManager implements ConfigSupervisor {

    private final List<ConfigWatcher> watchers = new ArrayList<>();

    private final Map<String, List<ConfigListener>> listeners = new HashMap<>();

    public ConfigWatcherManager() {
    }

    public ConfigWatcherManager(List<ConfigWatcher> watchers) {
        if (watchers != null) {
            this.watchers.addAll(watchers);
        }
    }

    @Override
    public void addListener(String type, ConfigListener listener) {
        if (type != null && listener != null) {
            listeners.computeIfAbsent(type, k -> new ArrayList<>()).add(listener);
            for (ConfigWatcher watcher : watchers) {
                watcher.addListener(type, listener);
            }
        }
    }

    @Override
    public void removeListener(String type, ConfigListener listener) {
        if (type != null && listener != null) {
            listeners.computeIfAbsent(type, k -> new ArrayList<>()).remove(listener);
            for (ConfigWatcher watcher : watchers) {
                watcher.removeListener(type, listener);
            }
        }
    }

    @Override
    public void addWatcher(ConfigWatcher watcher) {
        if (watcher != null) {
            watchers.add(watcher);
            for (Map.Entry<String, List<ConfigListener>> entry : listeners.entrySet()) {
                for (ConfigListener listener : entry.getValue()) {
                    watcher.addListener(entry.getKey(), listener);
                }
            }
        }
    }

    @Override
    public List<ConfigWatcher> getWatchers() {
        return Collections.unmodifiableList(watchers);
    }
}
