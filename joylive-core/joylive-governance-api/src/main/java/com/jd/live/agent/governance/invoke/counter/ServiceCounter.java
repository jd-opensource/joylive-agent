/*
 * Copyright © ${year} ${owner} (${email})
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jd.live.agent.governance.invoke.counter;

import com.jd.live.agent.core.util.time.Timer;
import com.jd.live.agent.governance.instance.Endpoint;
import lombok.Getter;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A class that represents a counter for a specific service, tracking the number of active, total, failed, and
 * successful requests, as well as the elapsed time for each request, for each endpoint and URI combination within
 * that service. It also provides methods to schedule and take snapshots of these counters, and to clean up counters
 * for endpoints that are no longer in use.
 */
public class ServiceCounter {

    private static final int DELAY_CLEAN = 1000;

    private static final long CLEAN_INTERVAL = 30000L;

    private static final int KEEP_TIME = 10000;

    private static final int SNAPSHOT_INTERVAL = 20000;

    private static final int SNAPSHOT_SPAN = 5000;

    @Getter
    private final String name;

    private final Timer timer;

    private long cleanTime;

    private final Map<String, EndpointCounter> counters = new ConcurrentHashMap<>();

    private final AtomicBoolean clean = new AtomicBoolean(false);

    public ServiceCounter(String name, Timer timer) {
        this.name = name;
        this.timer = timer;
        this.cleanTime = System.currentTimeMillis();
        scheduleSnapshot();
    }

    /**
     * Returns the Counter instance associated with the specified endpoint, creating a new one if it doesn't
     * already exist.
     *
     * @param endpoint The endpoint for which to retrieve the Counter.
     * @return The Counter instance.
     */
    public EndpointCounter getOrCreate(String endpoint) {
        return counters.computeIfAbsent(endpoint, e -> new EndpointCounter(e, this));
    }

    /**
     * Schedules a task to clean up counters for endpoints that are no longer in use, using the provided list of
     * current endpoints. The task will not be scheduled if one is already running.
     *
     * @param endpoints The list of current endpoints for the service.
     */
    public void tryClean(List<? extends Endpoint> endpoints) {
        if (System.currentTimeMillis() - cleanTime >= CLEAN_INTERVAL && clean.compareAndSet(false, true)) {
            cleanTime = System.currentTimeMillis();
            timer.delay("counter-clean-" + name, DELAY_CLEAN, () -> {
                clean(endpoints);
                cleanTime = System.currentTimeMillis();
                clean.set(false);
            });
        }
    }

    /**
     * Cleans up counters for endpoints that are no longer in use, based on the provided list of current endpoints.
     * Any counters associated with endpoints not in the list will be removed.
     *
     * @param endpoints The list of current endpoints for the service.
     */
    private void clean(List<? extends Endpoint> endpoints) {
        Set<String> exists = new HashSet<>(endpoints == null ? 0 : endpoints.size());
        if (endpoints != null) {
            for (Endpoint endpoint : endpoints) {
                exists.add(endpoint.getId());
            }
        }
        long time = System.currentTimeMillis();
        for (Map.Entry<String, EndpointCounter> entry : counters.entrySet()) {
            EndpointCounter counter = entry.getValue();
            if (!exists.contains(entry.getKey()) && time - counter.getAccessTime() > KEEP_TIME) {
                counters.remove(entry.getKey());
            }
        }
    }

    /**
     * Schedules a task to take a snapshot of all counters for this service. The task is scheduled with a random
     * delay between 20 and 30 seconds.
     */
    private void scheduleSnapshot() {
        int delay = SNAPSHOT_INTERVAL + ThreadLocalRandom.current().nextInt(SNAPSHOT_SPAN);
        timer.delay("counter-snapshot-" + name, delay, () -> {
            snapshot();
            scheduleSnapshot();
        });
    }

    /**
     * Takes a snapshot of all counters for this service.
     */
    private void snapshot() {
        for (EndpointCounter counter : counters.values()) {
            counter.snapshot();
        }
    }
}
