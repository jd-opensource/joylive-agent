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
package com.jd.live.agent.governance.policy.listener;

import com.jd.live.agent.core.config.ConfigEvent;
import com.jd.live.agent.core.config.ConfigEvent.EventType;
import com.jd.live.agent.core.config.ConfigWatcher;
import com.jd.live.agent.core.event.Event;
import com.jd.live.agent.core.event.Publisher;
import com.jd.live.agent.core.parser.ObjectParser;
import com.jd.live.agent.governance.policy.GovernancePolicy;
import com.jd.live.agent.governance.policy.PolicySubscriber;
import com.jd.live.agent.governance.policy.PolicySupervisor;
import com.jd.live.agent.governance.policy.service.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A listener class for service configuration updates that extends the AbstractListener class.
 */
public class ServiceListener extends AbstractListener<Service> {

    private final Map<String, PolicySubscriber> subscribers = new ConcurrentHashMap<>();

    private Set<String> loadedServices = new HashSet<>();

    private boolean loadedAll = false;

    private final Object mutex = new Object();

    public ServiceListener(PolicySupervisor supervisor,
                           ObjectParser parser,
                           Publisher<PolicySubscriber> publisher) {
        super(Service.class, supervisor, parser);
        publisher.addHandler(this::onEvent);
        supervisor.getSubscribers().forEach(this::subscribe);
    }

    @Override
    protected void updateItems(GovernancePolicy policy, List<Service> items, ConfigEvent event) {
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
                if (item.getVersion() > oldVersion) {
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
    protected void updateItem(GovernancePolicy policy, Service item, ConfigEvent event) {
        ServiceEvent se = (ServiceEvent) event;
        se.getLoadedServices().add(item.getName());
        List<Service> newServices = policy.onUpdate(Collections.singletonList(item), null, se.getMergePolicy(), se.getWatcher());
        policy.setServices(newServices);
    }

    @Override
    protected void deleteItem(GovernancePolicy policy, ConfigEvent event) {
        if (event.getName() == null) {
            return;
        }
        ServiceEvent se = (ServiceEvent) event;
        se.getLoadedServices().remove(event.getName());
        List<Service> newServices = policy.onDelete(se.getName(), se.getMergePolicy(), se.getWatcher());
        policy.setServices(newServices);
    }

    @Override
    protected synchronized void onSuccess(ConfigEvent event) {
        synchronized (mutex) {
            ServiceEvent se = (ServiceEvent) event;
            Set<String> loaded = se.getLoadedServices();
            loaded.addAll(loadedServices);
            loadedServices = loaded;
            loadedAll = loadedAll || event.getType() == EventType.UPDATE_ALL;
            subscribers.forEach((key, value) -> {
                if (loadedAll || loaded.contains(key)) {
                    value.complete(event.getWatcher());
                }
            });
        }
        super.onSuccess(event);

    }

    /**
     * Handles a list of events by subscribing to each event's policy subscriber.
     *
     * @param events A list of events containing policy subscribers to subscribe to.
     */
    private void onEvent(List<Event<PolicySubscriber>> events) {
        events.forEach(e -> subscribe(e.getData()));
    }

    /**
     * Subscribes to a policy subscriber, replacing any existing subscriber with the same unique name.
     *
     * @param subscriber The policy subscriber to subscribe to.
     */
    private void subscribe(PolicySubscriber subscriber) {
        if (subscriber != null && ConfigWatcher.TYPE_SERVICE_SPACE.equals(subscriber.getType())) {
            String name = subscriber.getName();
            PolicySubscriber old = subscribers.putIfAbsent(name, subscriber);
            if (old != null && old != subscriber) {
                old.trigger((v, t) -> {
                    if (t == null) {
                        subscriber.complete();
                    } else {
                        subscriber.completeExceptionally(t);
                    }
                });
            } else {
                synchronized (mutex) {
                    if (loadedAll || loadedServices.contains(name)) {
                        subscriber.complete();
                    }
                }
            }
        }
    }

}
