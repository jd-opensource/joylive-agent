package com.jd.live.agent.core.config;

import java.util.List;

/**
 * Config supervisor
 */
public interface PolicyWatcherSupervisor extends PolicyWatcher {

    /**
     * A constant representing the component name for the configuration watcher.
     */
    String COMPONENT_CONFIG_SUPERVISOR = "configSupervisor";

    /**
     * Adds a watcher to the list of watchers.
     *
     * @param watcher The watcher to add.
     */
    void addWatcher(PolicyWatcher watcher);

    /**
     * Returns a list of PolicyWatcher instances that are currently registered to watch for policy changes.
     *
     * @return A list of PolicyWatcher instances.
     */
    List<PolicyWatcher> getWatchers();
}
