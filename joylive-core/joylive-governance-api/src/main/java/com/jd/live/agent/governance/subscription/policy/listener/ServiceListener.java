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
package com.jd.live.agent.governance.subscription.policy.listener;

import com.jd.live.agent.core.event.Event;
import com.jd.live.agent.core.event.Publisher;
import com.jd.live.agent.core.parser.ObjectParser;
import com.jd.live.agent.governance.policy.GovernancePolicy;
import com.jd.live.agent.governance.policy.PolicySubscription;
import com.jd.live.agent.governance.policy.PolicySupervisor;
import com.jd.live.agent.governance.policy.service.Service;
import com.jd.live.agent.governance.subscription.policy.PolicyEvent;
import com.jd.live.agent.governance.subscription.policy.PolicyEvent.EventType;
import com.jd.live.agent.governance.subscription.policy.PolicyWatcher;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A listener class for service configuration updates that extends the AbstractListener class.
 */
public class ServiceListener extends AbstractListener<Service> {

    private final Map<String, PolicySubscription> subscribers = new ConcurrentHashMap<>();

    private final Map<String, PolicyLoader> loaders = new HashMap<>();

    private final Object mutex = new Object();

    public ServiceListener(PolicySupervisor supervisor, ObjectParser parser, Publisher<PolicySubscription> publisher) {
        super(Service.class, supervisor, parser);
        // Register the listener with the publisher
        publisher.addHandler(this::onEvent);
        // Subscribe to all policies
        supervisor.getSubscriptions().forEach(this::subscribe);
    }

    @Override
    protected void updateItems(GovernancePolicy policy, List<Service> items, PolicyEvent event) {
        ServiceEvent se = (ServiceEvent) event;
        Map<String, Long> newVersions = new HashMap<>(items == null ? 0 : items.size());
        List<Service> oldServices = policy.getServices();
        Map<String, Long> oldVersions = new HashMap<>(oldServices == null ? 0 : oldServices.size());
        if (oldServices != null) {
            oldServices.forEach(s -> oldVersions.put(s.getName(), s.getVersion()));
        }
        List<Service> updates = new ArrayList<>();
        Set<String> deletes = new HashSet<>();
        // AddOrUpdate
        if (items != null) {
            Set<String> loadedServices = se.getLoadedServices();
            for (Service item : items) {
                loadedServices.add(item.getName());
                newVersions.put(item.getName(), item.getVersion());
                long oldVersion = oldVersions.getOrDefault(item.getName(), -1L);
                if (item.getVersion() != oldVersion) {
                    updates.add(item);
                }
            }
        }
        // Remove
        for (String name : oldVersions.keySet()) {
            if (!newVersions.containsKey(name)) {
                deletes.add(name);
            }
        }

        List<Service> newServices = policy.onUpdate(updates, deletes, se.getMergePolicy(), se.getWatcher());
        policy.setServices(newServices);
    }

    @Override
    protected void updateItem(GovernancePolicy policy, Service item, PolicyEvent event) {
        ServiceEvent se = (ServiceEvent) event;
        se.getLoadedServices().add(item.getName());
        List<Service> newServices = policy.onUpdate(Collections.singletonList(item), null, se.getMergePolicy(), se.getWatcher());
        policy.setServices(newServices);
    }

    @Override
    protected void deleteItem(GovernancePolicy policy, PolicyEvent event) {
        if (event.getName() == null) {
            return;
        }
        ServiceEvent se = (ServiceEvent) event;
        se.getLoadedServices().remove(event.getName());
        List<Service> newServices = policy.onDelete(se.getName(), se.getMergePolicy(), se.getWatcher());
        policy.setServices(newServices);
    }

    @Override
    protected void onSuccess(PolicyEvent event) {
        ServiceEvent se = (ServiceEvent) event;
        synchronized (mutex) {
            PolicyLoader loader = loaders.computeIfAbsent(event.getWatcher(), PolicyLoader::new);
            Set<String> names = se.getLoadedServices();
            if (event.getType() == EventType.UPDATE_ALL) {
                if (!loader.isLoaded()) {
                    loader.setLoaded(true);
                    subscribers.forEach((name, subscriber) -> subscriber.complete(event.getWatcher()));
                    return;
                }
            }
            if (names != null) {
                for (String name : names) {
                    if (loader.addLoaded(name)) {
                        PolicySubscription subscription = subscribers.get(name);
                        if (subscription != null) {
                            subscription.complete(event.getWatcher());
                        }
                    }
                }
            }
        }
    }

    /**
     * Handles a list of events by subscribing to each event's policy subscriber.
     *
     * @param events A list of events containing policy subscribers to subscribe to.
     */
    private void onEvent(List<Event<PolicySubscription>> events) {
        events.forEach(e -> subscribe(e.getData()));
    }

    /**
     * Subscribes to a policy subscription, replacing any existing subscription with the same unique name.
     *
     * @param subscription The policy subscription to subscribe to.
     */
    private void subscribe(PolicySubscription subscription) {
        if (subscription != null && PolicyWatcher.TYPE_SERVICE_POLICY.equals(subscription.getType())) {
            String name = subscription.getName();
            PolicySubscription old = subscribers.putIfAbsent(name, subscription);
            if (old != null && old != subscription) {
                // Also known as it will never happen.
                old.watch().whenComplete((v, t) -> {
                    if (t == null) {
                        subscription.complete();
                    } else {
                        subscription.completeExceptionally(t);
                    }
                });
            } else {
                synchronized (mutex) {
                    for (PolicyLoader loader : loaders.values()) {
                        if (loader.isLoaded(name)) {
                            subscription.complete(loader.getName());
                        }
                    }
                }
            }
        }
    }

    /**
     * Helper class for loading and tracking policy states.
     */
    private static class PolicyLoader {

        @Getter
        private final String name;

        @Getter
        @Setter
        private boolean loaded;

        private final Set<String> loadedServices = new HashSet<>();

        PolicyLoader(String name) {
            this.name = name;
        }

        public boolean addLoaded(String name) {
            return loadedServices.add(name);
        }

        public boolean isLoaded(String name) {
            return loaded || loadedServices.contains(name);
        }
    }

}
