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
package com.jd.live.agent.governance.invoke.permission;

import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.util.AtomicUtils;
import com.jd.live.agent.core.util.Close;
import com.jd.live.agent.core.util.time.Timer;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.config.RecyclerConfig;
import com.jd.live.agent.governance.policy.PolicyVersion;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * An abstract class that serves as a factory for creating and managing instances of
 * {@link Licensee}. It maintains a map of licensees and provides methods to retrieve,
 * create, and recycle them based on policy versions and expiration times.
 *
 * @param <P> the type of policy version
 * @param <K> the type of key used to identify licensees
 * @param <V> the type of licensee
 */
public abstract class AbstractLicenseeFactory<P extends PolicyVersion, K, V extends Licensee<P>> {

    /**
     * A map that stores licensees with their keys. Each value is an {@link AtomicReference}
     * to a {@link Licensee}.
     */
    protected final Map<K, AtomicReference<V>> licensees = new ConcurrentHashMap<>();

    /**
     * A timer used to schedule recurring tasks for recycling licensees.
     */
    @Inject(Timer.COMPONENT_TIMER)
    protected Timer timer;

    /**
     * Configuration related to governance settings.
     */
    @Inject(GovernanceConfig.COMPONENT_GOVERNANCE_CONFIG)
    protected GovernanceConfig governanceConfig;

    /**
     * A flag to indicate whether the recycler has been added.
     */
    protected final AtomicBoolean recycled = new AtomicBoolean(false);

    /**
     * Returns the configuration for the recycler.
     *
     * @return the recycler configuration
     */
    protected abstract RecyclerConfig getConfig();

    /**
     * Retrieves a licensee for a given policy and key. If the policy is null or does not
     * satisfy the predicate, it returns null. If a valid licensee exists with a version
     * greater than or equal to the policy version, it returns that licensee. Otherwise,
     * it creates a new licensee using the provided creator.
     *
     * @param policy    the policy version
     * @param key       the key to identify the licensee
     * @param predicate a predicate to test the policy
     * @param creator   a supplier to create a new licensee
     * @return the licensee or null if not applicable
     */
    protected V get(P policy, K key, Predicate<P> predicate, Supplier<V> creator) {
        if (policy == null || key == null || predicate != null && !predicate.test(policy)) {
            return null;
        }
        V result = AtomicUtils.getOrUpdate(licensees, key, creator, policy::isOlderThan, this::onSuccess);
        if (result != null) {
            P old = result.getPolicy();
            if (old != policy && old.getVersion() == policy.getVersion()) {
                // exchange the new policy.
                result.exchange(policy);
            }
        }
        return result;
    }

    /**
     * Handles the success case when a new value is successfully set.
     *
     * @param oldValue the old value that is being replaced
     * @param newValue the new value that has been set
     */
    protected void onSuccess(V oldValue, V newValue) {
        Close.instance().close(oldValue);
        if (recycled.compareAndSet(false, true)) {
            addRecycler(getTaskName());
        }
    }

    /**
     * Generates a name for the recycler task based on the recycler configuration class name.
     *
     * @return the name of the recycler task
     */
    protected String getTaskName() {
        String name = getConfig().getClass().getSimpleName();
        name = name.replace("Config", "");
        name = "Recycle-" + name;
        return name;
    }

    /**
     * Schedules a recurring task to recycle licensees based on their expiration time.
     * This method retrieves the clean interval from the configuration and sets up a delayed task
     * that calls the {@link #recycle()} method and reschedules itself.
     */
    protected void addRecycler(String name) {
        timer.delay(name, getConfig().getCleanInterval(), () -> {
            recycle();
            addRecycler(name);
        });
    }

    /**
     * Recycles expired licensee. This method checks each licensee to see if it has
     * expired based on the current time and the configured expiration time. If a rate limiter
     * has exceeded its expiration time, it is removed from the collection.
     */
    protected void recycle() {
        long expireTime = getConfig().getExpireTime();
        Close closer = Close.instance();
        licensees.entrySet().removeIf(entry -> {
            V licensee = entry.getValue().get();
            if (licensee != null && licensee.isExpired(expireTime)) {
                // close first to avoid concurrent modification exception
                closer.close(licensee);
                return true;
            }
            return false;
        });
    }

}

