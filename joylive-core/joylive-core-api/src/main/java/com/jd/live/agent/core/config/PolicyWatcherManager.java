package com.jd.live.agent.core.config;

import java.util.*;

/**
 * Config watcher manager
 */
public class PolicyWatcherManager implements PolicyWatcherSupervisor {

    private final List<PolicyWatcher> watchers = new ArrayList<>();

    private final Map<String, List<PolicyListener>> listeners = new HashMap<>();

    public PolicyWatcherManager() {
    }

    public PolicyWatcherManager(List<PolicyWatcher> watchers) {
        if (watchers != null) {
            this.watchers.addAll(watchers);
        }
    }

    @Override
    public void addListener(String type, PolicyListener listener) {
        if (type != null && listener != null) {
            listeners.computeIfAbsent(type, k -> new ArrayList<>()).add(listener);
            for (PolicyWatcher watcher : watchers) {
                watcher.addListener(type, listener);
            }
        }
    }

    @Override
    public void removeListener(String type, PolicyListener listener) {
        if (type != null && listener != null) {
            listeners.computeIfAbsent(type, k -> new ArrayList<>()).remove(listener);
            for (PolicyWatcher watcher : watchers) {
                watcher.removeListener(type, listener);
            }
        }
    }

    @Override
    public void addWatcher(PolicyWatcher watcher) {
        if (watcher != null) {
            watchers.add(watcher);
            for (Map.Entry<String, List<PolicyListener>> entry : listeners.entrySet()) {
                for (PolicyListener listener : entry.getValue()) {
                    watcher.addListener(entry.getKey(), listener);
                }
            }
        }
    }

    @Override
    public List<PolicyWatcher> getWatchers() {
        return Collections.unmodifiableList(watchers);
    }
}
