package com.jd.live.agent.governance.subscription.policy;

import java.util.List;

/**
 * Config supervisor
 */
public interface PolicyWatcherSupervisor extends PolicyWatcher {

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
