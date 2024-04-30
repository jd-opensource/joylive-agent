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
import com.jd.live.agent.governance.policy.GovernancePolicy;
import com.jd.live.agent.governance.policy.PolicySubscriber;
import com.jd.live.agent.governance.policy.PolicySupervisor;
import com.jd.live.agent.governance.policy.service.Service;

import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * PolicyService
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
@Injectable
@Extension("LiveSpaceFileSyncer")
@ConditionalOnProperty(name = SyncConfig.SYNC_MICROSERVICE_TYPE, value = "file")
public class MicroServiceFileSyncer extends AbstractFileSyncer<List<Service>> {

    private static final Logger logger = LoggerFactory.getLogger(MicroServiceFileSyncer.class);

    private static final String CONFIG_MICROSERVICE = "microservice.json";

    @Inject(PolicySupervisor.COMPONENT_POLICY_SUPERVISOR)
    private PolicySupervisor policySupervisor;

    @Inject(Publisher.POLICY_SUBSCRIBER)
    protected Publisher<PolicySubscriber> publisher;

    @Config(SyncConfig.SYNC_MICROSERVICE)
    private SyncConfig syncConfig = new SyncConfig();

    private final Map<String, PolicySubscriber> subscribers = new ConcurrentHashMap<>();
    private final EventHandler<PolicySubscriber> handler = this::onEvent;
    private final AtomicBoolean loaded = new AtomicBoolean();

    @Override
    protected String getName() {
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
        return result == null || result.isEmpty() ? CONFIG_MICROSERVICE : result;
    }

    @Override
    protected boolean updateOnce(List<Service> value, FileDigest meta) {
        GovernancePolicy expect = policySupervisor.getPolicy();
        GovernancePolicy update = expect == null ? new GovernancePolicy() : expect.copy();
        update.setServices(value);
        update.cache();
        if (policySupervisor.update(expect, update)) {
            logger.info("success synchronizing file " + file.getPath());
            if (loaded.compareAndSet(false, true)) {
                for (PolicySubscriber subscriber : subscribers.values()) {
                    subscriber.complete();
                }
            }
            return true;
        }
        return false;
    }

    private void onEvent(List<Event<PolicySubscriber>> events) {
        for (Event<PolicySubscriber> event : events) {
            subscribe(event.getData());
        }
    }

    private void subscribe(PolicySubscriber subscriber) {
        if (subscribers.putIfAbsent(subscriber.getName(), subscriber) == null && loaded.get()) {
            subscriber.complete();
        }
    }

}
