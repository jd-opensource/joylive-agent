package com.jd.live.agent.core.config;

import java.util.List;

/**
 * Config supervisor
 */
public interface ConfigSupervisor extends ConfigWatcher {

    /**
     * A constant representing the component name for the configuration watcher.
     */
    String COMPONENT_CONFIG_SUPERVISOR = "configSupervisor";

    /**
     * Adds a watcher to the list of watchers.
     *
     * @param watcher The watcher to add.
     */
    void addWatcher(ConfigWatcher watcher);

    List<ConfigWatcher> getWatchers();
}
