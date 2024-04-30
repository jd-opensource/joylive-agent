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
package com.jd.live.agent.bootstrap.bytekit.advice;

import com.jd.live.agent.bootstrap.plugin.PluginEvent;
import com.jd.live.agent.bootstrap.plugin.PluginListener;
import com.jd.live.agent.bootstrap.plugin.PluginPublisher;
import com.jd.live.agent.bootstrap.plugin.definition.Interceptor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Represents a descriptor for advice, implementing the PluginListener interface.
 * This class is responsible for managing interceptors and ensuring thread-safe operations
 * related to plugin events.
 */
public class AdviceDesc implements PluginListener {
    /**
     * A unique key identifying the advice.
     */
    private final String key;

    /**
     * A list of interceptors associated with this advice.
     */
    private final List<Interceptor> interceptors = new ArrayList<>();

    /**
     * An atomic reference used for thread-safe operations, primarily to manage the owner of this advice.
     */
    private final AtomicReference<Object> reference = new AtomicReference<>();

    /**
     * A set of interceptor names ensuring that each interceptor is unique.
     */
    private final Set<String> names = new HashSet<>();

    /**
     * Constructs a new AdviceDesc instance with a specified key.
     *
     * @param key the unique key for the advice
     */
    public AdviceDesc(String key) {
        this.key = key;
    }

    /**
     * Returns the unique key of this advice.
     *
     * @return the unique key
     */
    public String getKey() {
        return key;
    }

    /**
     * Retrieves the list of interceptors associated with this advice.
     *
     * @return the list of interceptors
     */
    public List<Interceptor> getInterceptors() {
        return interceptors;
    }

    /**
     * Attempts to add an interceptor to this advice. Ensures that the interceptor is not null
     * and that its name is unique before adding it to the list of interceptors.
     *
     * @param interceptor the interceptor to be added
     * @return true if the interceptor was added successfully, false otherwise
     */
    public boolean add(Interceptor interceptor) {
        if (interceptor != null && add(interceptor.getClass().getCanonicalName())) {
            interceptors.add(interceptor);
            return true;
        }
        return false;
    }

    /**
     * Locks this advice to a specific owner, ensuring that only the owner can perform certain operations.
     * If the owner is an instance of PluginPublisher, it also registers this advice as a listener.
     *
     * @param owner the object attempting to lock this advice
     * @return true if the advice was successfully locked to the owner, false otherwise
     */
    public boolean lock(Object owner) {
        if (owner == null) {
            return false;
        } else if (reference.compareAndSet(null, owner)) {
            if (owner instanceof PluginPublisher) {
                ((PluginPublisher) owner).addListener(this);
            }
            return true;
        } else {
            return reference.get() == owner;
        }
    }

    /**
     * Unlocks this advice, allowing it to be locked by another owner.
     *
     * @param owner the current owner attempting to unlock
     * @return true if successfully unlocked, false otherwise
     */
    protected boolean unlock(Object owner) {
        return owner != null && reference.compareAndSet(owner, null);
    }

    /**
     * Adds an interceptor's name to the set of names, ensuring it is unique.
     *
     * @param interceptor the name of the interceptor to add
     * @return true if the name was added successfully, false if it already exists
     */
    protected boolean add(String interceptor) {
        return names.add(interceptor);
    }

    /**
     * Handles plugin events, specifically listening for uninstall events to remove the advice.
     *
     * @param event the plugin event that occurred
     */
    @Override
    public void onEvent(PluginEvent event) {
        if (event.getType() == PluginEvent.EventType.UNINSTALL && unlock(event.getOwner())) {
            AdviceHandler.remove(getKey());
        }
    }
}

