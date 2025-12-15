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
package com.jd.live.agent.governance.subscription.config.gray;

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.parser.ObjectParser;
import com.jd.live.agent.core.util.Locks;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Watches for configuration changes and automatically switches between release and beta configurations based on policy matching.
 */
public abstract class ConfigWatcher<C, L> extends ConfigFetcher<C> {

    /**
     * Logger instance for this class.
     */
    protected static final Logger logger = LoggerFactory.getLogger(ConfigWatcher.class);

    /**
     * Initial state constant.
     */
    protected static final int STATE_INIT = 0;

    /**
     * Beta configuration state constant.
     */
    protected static final int STATE_BETA = 1;

    /**
     * Release configuration state constant.
     */
    protected static final int STATE_RELEASE = 2;

    /**
     * Update event listener.
     */
    protected final L onUpdate;

    /**
     * Policy event listener.
     */
    protected final L onPolicy;

    /**
     * Configuration change listeners.
     */
    protected final Set<ConfigListener<L>> listeners = new CopyOnWriteArraySet<>();

    /**
     * Current configuration state.
     */
    protected final AtomicInteger state = new AtomicInteger(STATE_INIT);

    /**
     * Subscription status flag.
     */
    protected final AtomicBoolean subscribed = new AtomicBoolean(false);

    /**
     * Thread-safe lock for subscription operations.
     */
    protected final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Synchronization object for state updates.
     */
    protected final Object mutex = new Object();

    /**
     * Beta configuration key.
     */
    protected ConfigKey keyBeta;

    public ConfigWatcher(C client, ConfigKey key, Application application, ObjectParser json) {
        super(client, key, application, json);
        this.onUpdate = createOnUpdateListener();
        this.onPolicy = createOnPolicyListener();
    }

    /**
     * Checks if currently subscribed to configuration changes.
     *
     * @return true if subscribed, false otherwise
     */
    public boolean isSubscribed() {
        return subscribed.get();
    }

    /**
     * Starts watching for config changes.
     *
     * @throws Exception if initial setup fails
     */
    public void subscribe() throws Exception {
        Locks.write(lock, () -> {
            doSubscribe();
            return null;
        });
    }

    /**
     * Stops all config watches.
     */
    public void unsubscribe() {
        Locks.write(lock, () -> doUnsubscribe());
    }

    /**
     * Resubscribes by resetting state and recreating subscription.
     * Unsubscribes first to avoid duplicate listeners.
     *
     * @throws Exception if subscription fails
     */
    public void resubscribe() throws Exception {
        Locks.write(lock, () -> {
            // Since connection recovery keeps the state, we need to initialize the state
            state.set(STATE_INIT);
            // unsubscribe first to avoid add duplicated listeners.
            doUnsubscribe();
            doSubscribe();
            return null;
        });
    }

    /**
     * Adds a configuration change listener.
     *
     * @param listener the listener to add
     * @return true if added, false if null or already exists
     */
    public boolean addListener(L listener) {
        if (listener == null) {
            return false;
        }
        synchronized (mutex) {
            ConfigListener<L> configListener = new ConfigListener<>(listener);
            if (listeners.add(configListener) && isSubscribed()) {
                doUpdateConfig(configListener, config.get());
                return true;
            }
            return false;
        }
    }

    /**
     * Removes a configuration change listener.
     *
     * @param listener the listener to remove
     * @return true if removed, false if null or not found
     */
    public boolean removeListener(L listener) {
        if (listener == null) {
            return false;
        }
        synchronized (mutex) {
            return listeners.remove(new ConfigListener<>(listener));
        }
    }

    /**
     * Checks if no listeners are registered.
     *
     * @return true if no listeners, false otherwise
     */
    public boolean isEmpty() {
        return listeners.isEmpty();
    }

    /**
     * Creates listener for policy configuration updates.
     *
     * @return policy update listener
     */
    protected abstract L createOnPolicyListener();

    /**
     * Creates listener for configuration updates.
     *
     * @return config update listener
     */
    protected abstract L createOnUpdateListener();

    /**
     * Subscribes to gray policy configuration changes.
     * Atomically sets subscription status and establishes policy config monitoring.
     * Performs cleanup on subscription failure to maintain consistent state.
     */
    protected void doSubscribe() throws Exception {
        if (subscribed.compareAndSet(false, true)) {
            logger.info("Subscribe gray policy {}@{}", keyPolicy.getName(), keyPolicy.getGroup());
            try {
                subscribe(keyPolicy, onPolicy);
                //String value = doGetConfig(keyPolicy, 0);
                //onUpdatePolicy(value);
            } catch (Exception e) {
                subscribed.set(false);
                doUnsubscribe();
                throw e;
            }
        }
    }

    /**
     * Unsubscribes from all configuration changes.
     * Removes listeners for release, policy and beta configurations.
     */
    protected void doUnsubscribe() {
        if (subscribed.compareAndSet(true, false)) {
            unsubscribe(keyRelease, onUpdate);
            unsubscribe(keyPolicy, onPolicy);
            unsubscribe(keyBeta, onUpdate);
        }
    }

    /**
     * Unsubscribes from configuration changes for specified key.
     *
     * @param key      configuration key
     * @param listener update listener
     */
    protected void subscribe(ConfigKey key, L listener) throws Exception {
        if (key != null) {
            doSubscribe(key, listener);
        }
    }

    /**
     * Subscribes to configuration changes for specified key.
     *
     * @param key      configuration key
     * @param listener update listener
     * @throws Exception if subscription fails
     */
    protected abstract void doSubscribe(ConfigKey key, L listener) throws Exception;

    /**
     * Unsubscribes from configuration changes for specified key.
     *
     * @param key      configuration key
     * @param listener update listener
     */
    protected void unsubscribe(ConfigKey key, L listener) {
        if (key != null) {
            doUnsubscribe(key, listener);
        }
    }

    /**
     * Unsubscribes from configuration changes for specified key.
     *
     * @param key      configuration key
     * @param listener update listener
     */
    protected abstract void doUnsubscribe(ConfigKey key, L listener);

    /**
     * Handles policy config updates.
     *
     * @param value New policy config JSON
     * @throws Exception if policy processing fails
     */
    protected void onUpdatePolicy(String value) throws Exception {
        Locks.read(lock, () -> {
            if (isSubscribed()) {
                synchronized (mutex) {
                    doUpdatePolicy(value);
                }
            }
            return null;
        });
    }

    /**
     * Processes policy configuration updates and switches between release/beta configs.
     *
     * @param value policy configuration JSON
     * @throws Exception if policy processing fails
     */
    protected void doUpdatePolicy(String value) throws Exception {
        ConfigPolicy policy = parsePolicy(value);
        if (policy != null) {
            ConfigKey newKeyBeta = new ConfigKey(policy.getName(), keyRelease.getGroup());
            if (state.compareAndSet(STATE_RELEASE, STATE_BETA)
                    || state.compareAndSet(STATE_INIT, STATE_BETA)
                    || state.get() == STATE_BETA && !newKeyBeta.equals(keyBeta)) {
                logger.info("Subscribe gray config {}@{}", newKeyBeta.getName(), newKeyBeta.getGroup());
                unsubscribe(keyRelease, onUpdate);
                unsubscribe(keyBeta, onUpdate);
                keyBeta = newKeyBeta;
                subscribe(keyBeta, onUpdate);
                //doUpdateConfig(new ConfigVersion(doGetConfig(keyBeta, 0), version.get() + 1));
            }
        } else if (state.compareAndSet(STATE_BETA, STATE_RELEASE)
                || state.compareAndSet(STATE_INIT, STATE_RELEASE)) {
            logger.info("Subscribe release config {}@{}", keyRelease.getName(), keyRelease.getGroup());
            unsubscribe(keyBeta, onUpdate);
            subscribe(keyRelease, onUpdate);
            //doUpdateConfig(new ConfigVersion(doGetConfig(keyRelease, 0), version.get() + 1));
        }
    }

    /**
     * Handles config value updates.
     *
     * @param value New configuration value
     */
    protected void onUpdateConfig(String value) {
        Locks.read(lock, () -> {
            if (isSubscribed()) {
                synchronized (mutex) {
                    doUpdateConfig(new ConfigVersion(value, version.incrementAndGet()));
                }
            }
        });
    }

    /**
     * Updates current configuration value and notifies all listeners.
     *
     * @param newer new configuration value
     */
    protected void doUpdateConfig(ConfigVersion newer) {
        ConfigVersion older = config.get();
        if (older == null || newer.getVersion() > older.getVersion()) {
            config.set(newer);
            listeners.forEach(i -> doUpdateConfig(i, newer));
        }
    }

    /**
     * Updates configuration for specified listener.
     *
     * @param listener target listener
     * @param version  new configuration value
     */
    protected void doUpdateConfig(ConfigListener<L> listener, ConfigVersion version) {
        if (version == null || version.getVersion() <= listener.getVersion()) {
            return;
        }
        try {
            doUpdateConfig(listener.getListener(), version.getValue());
            listener.setVersion(version.getVersion());
        } catch (Exception e) {
            logger.error("Failed to update config {} for listener {}, caused by {}", keyRelease, listener, e.getMessage(), e);
        }
    }

    /**
     * Updates configuration for specified listener.
     *
     * @param listener target listener
     * @param value    new configuration value
     */
    protected abstract void doUpdateConfig(L listener, String value);
}
