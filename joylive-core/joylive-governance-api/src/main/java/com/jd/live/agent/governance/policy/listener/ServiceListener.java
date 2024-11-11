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

import com.jd.live.agent.core.config.ConfigWatcher;
import com.jd.live.agent.core.config.Configuration;
import com.jd.live.agent.core.event.Event;
import com.jd.live.agent.core.event.EventHandler;
import com.jd.live.agent.core.event.Publisher;
import com.jd.live.agent.core.parser.ObjectParser;
import com.jd.live.agent.core.parser.TypeReference;
import com.jd.live.agent.governance.policy.GovernancePolicy;
import com.jd.live.agent.governance.policy.PolicySubscriber;
import com.jd.live.agent.governance.policy.PolicySupervisor;
import com.jd.live.agent.governance.policy.service.MergePolicy;
import com.jd.live.agent.governance.policy.service.Service;

import java.io.StringReader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class ServiceListener extends AbstractListener<List<Service>> {

    private final Publisher<PolicySubscriber> publisher;

    private final MergePolicy mergePolicy;

    private final ObjectParser parser;

    private final Map<String, PolicySubscriber> subscribers = new ConcurrentHashMap<>();

    private final EventHandler<PolicySubscriber> handler = this::onEvent;

    private final AtomicBoolean loaded = new AtomicBoolean();

    private Map<String, Long> versions = new HashMap<>();

    private Map<String, Long> newVersions;

    public ServiceListener(PolicySupervisor supervisor,
                           Publisher<PolicySubscriber> publisher,
                           MergePolicy mergePolicy,
                           ObjectParser parser) {
        super(supervisor);
        this.publisher = publisher;
        this.mergePolicy = mergePolicy;
        this.parser = parser;
        publisher.addHandler(handler);
        supervisor.getSubscribers().forEach(this::subscribe);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected List<Service> parse(Configuration config) {
        Object value = config.getValue();
        if (value instanceof List) {
            return (List<Service>) value;
        } else if (value instanceof Service) {
            List<Service> result = new ArrayList<>();
            result.add((Service) value);
            return result;
        } else if (value instanceof String) {
            String str = (String) value;
            str = str.trim();
            if (str.isEmpty()) {
                return new ArrayList<>();
            } else if (str.startsWith("[")) {
                return parser.read(new StringReader(str), new TypeReference<List<Service>>() {
                });
            } else {
                List<Service> result = new ArrayList<>();
                result.add(parser.read(new StringReader(str), Service.class));
                return result;
            }
        } else {
            return new ArrayList<>();
        }
    }

    @Override
    protected void update(GovernancePolicy policy, List<Service> services, String watcher) {
        List<Service> updates = new ArrayList<>();
        Set<String> deletes = new HashSet<>();
        newVersions = new HashMap<>();
        // AddOrUpdate
        if (services != null && !services.isEmpty()) {
            for (Service service : services) {
                newVersions.put(service.getName(), service.getVersion());
                long oldVersion = versions.getOrDefault(service.getName(), -1L);
                if (service.getVersion() > oldVersion) {
                    updates.add(service);
                }
            }
        }
        // Remove
        for (String name : versions.keySet()) {
            if (!newVersions.containsKey(name)) {
                deletes.add(name);
            }
        }

        policy.setServices(policy.onUpdate(updates, deletes, mergePolicy, watcher));
    }

    @Override
    protected void onSuccess(Configuration config) {
        versions = newVersions;
        newVersions = null;
        if (loaded.compareAndSet(false, true)) {
            subscribers.forEach((key, value) -> value.complete(config.getWatcher()));
        }
        super.onSuccess(config);
    }

    private void onEvent(List<Event<PolicySubscriber>> events) {
        events.forEach(e -> subscribe(e.getData()));
    }

    private void subscribe(PolicySubscriber subscriber) {
        if (subscriber != null && ConfigWatcher.TYPE_SERVICE_SPACE.equals(subscriber.getType())) {
            PolicySubscriber old = subscribers.putIfAbsent(subscriber.getUniqueName(), subscriber);
            if (old != null && old != subscriber) {
                old.trigger((v, t) -> {
                    if (t == null) {
                        subscriber.complete(getName());
                    } else {
                        subscriber.completeExceptionally(t);
                    }
                });
            }
        }
    }
}
