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
package com.jd.live.agent.implement.service.policy.file;

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.config.SyncConfig;
import com.jd.live.agent.core.event.Event;
import com.jd.live.agent.core.event.EventHandler;
import com.jd.live.agent.core.event.Publisher;
import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Config;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.parser.TypeReference;
import com.jd.live.agent.core.service.AbstractFileSyncer;
import com.jd.live.agent.core.service.file.FileDigest;
import com.jd.live.agent.governance.policy.GovernancePolicy;
import com.jd.live.agent.governance.policy.PolicySubscriber;
import com.jd.live.agent.governance.policy.PolicySupervisor;
import com.jd.live.agent.governance.policy.PolicyType;
import com.jd.live.agent.governance.policy.service.Service;
import com.jd.live.agent.governance.service.PolicyService;
import com.jd.live.agent.implement.service.policy.file.config.ServiceSyncConfig;

import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * ServiceFileSyncer
 */
@Injectable
@Extension("ServiceFileSyncer")
@ConditionalOnProperty(name = SyncConfig.SYNC_MICROSERVICE_TYPE, value = "file")
public class ServiceFileSyncer extends AbstractFileSyncer<List<Service>> implements PolicyService {

    private static final Logger logger = LoggerFactory.getLogger(ServiceFileSyncer.class);

    private static final String CONFIG_MICROSERVICE = "microservice.json";

    @Inject(PolicySupervisor.COMPONENT_POLICY_SUPERVISOR)
    private PolicySupervisor policySupervisor;

    @Inject(Publisher.POLICY_SUBSCRIBER)
    protected Publisher<PolicySubscriber> publisher;

    @Config(SyncConfig.SYNC_MICROSERVICE)
    private ServiceSyncConfig syncConfig = new ServiceSyncConfig();

    private final Map<String, PolicySubscriber> subscribers = new ConcurrentHashMap<>();

    private final EventHandler<PolicySubscriber> handler = this::onEvent;

    private final AtomicBoolean loaded = new AtomicBoolean();

    private Map<String, Long> versions = new HashMap<>();

    @Override
    public PolicyType getPolicyType() {
        return PolicyType.SERVICE_POLICY;
    }

    @Override
    public String getName() {
        return "service-syncer";
    }

    @Override
    protected CompletableFuture<Void> doStart() {
        publisher.addHandler(handler);
        policySupervisor.getSubscribers().forEach(this::subscribe);
        return super.doStart();
    }

    @Override
    protected CompletableFuture<Void> doStop() {
        publisher.removeHandler(handler);
        return super.doStop();
    }

    @Override
    protected List<Service> parse(InputStreamReader reader) {
        return jsonParser.read(reader, new TypeReference<List<Service>>() {
        });
    }

    @Override
    protected SyncConfig getSyncConfig() {
        return syncConfig;
    }

    @Override
    protected String getResource(SyncConfig config) {
        String result = super.getResource(config);
        return isConfigFile(result) ? result : CONFIG_MICROSERVICE;
    }

    @Override
    protected boolean updateOnce(List<Service> services, FileDigest meta) {
        List<Service> updates = new ArrayList<>();
        Set<String> deletes = new HashSet<>();
        Map<String, Long> newVersions = new HashMap<>();
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

        if (policySupervisor.update(policy -> newPolicy(updates, deletes, policy))) {
            versions = newVersions;
            logger.info("Success synchronizing file " + file.getPath());
            onLoaded();
            return true;
        }
        return false;
    }

    private GovernancePolicy newPolicy(List<Service> updates, Set<String> deletes, GovernancePolicy policy) {
        GovernancePolicy result = policy == null ? new GovernancePolicy() : policy.copy();
        result.setServices(result.onUpdate(updates, deletes, syncConfig.getPolicy(), getName()));
        return result;
    }

    private void onLoaded() {
        if (loaded.compareAndSet(false, true)) {
            for (PolicySubscriber subscriber : subscribers.values()) {
                subscriber.complete(getName());
            }
        }
    }

    private void onEvent(List<Event<PolicySubscriber>> events) {
        events.forEach(e -> subscribe(e.getData()));
    }

    private void subscribe(PolicySubscriber subscriber) {
        if (subscriber != null && subscriber.getType() == PolicyType.SERVICE_POLICY) {
            if (!isStarted()) {
                subscriber.completeExceptionally(new IllegalStateException("Microservice has been stopped"));
            } else {
                PolicySubscriber old = subscribers.putIfAbsent(subscriber.getName(), subscriber);
                if (loaded.get()) {
                    subscriber.complete(getName());
                } else if (old != null && old != subscriber) {
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
}
