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
package com.jd.live.agent.implement.service.policy.multilive;

import com.jd.live.agent.core.config.TimerConfig;
import com.jd.live.agent.core.event.EventBus;
import com.jd.live.agent.core.event.Publisher;
import com.jd.live.agent.core.instance.AppService;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.instance.Location;
import com.jd.live.agent.core.parser.TypeReference;
import com.jd.live.agent.core.util.CollectionUtils;
import com.jd.live.agent.core.util.http.HttpResponse;
import com.jd.live.agent.core.util.http.HttpStatus;
import com.jd.live.agent.core.util.time.TimeScheduler;
import com.jd.live.agent.core.util.time.Timer;
import com.jd.live.agent.governance.config.SyncConfig;
import com.jd.live.agent.governance.policy.GovernancePolicy;
import com.jd.live.agent.governance.policy.PolicyManager;
import com.jd.live.agent.governance.policy.PolicySubscription;
import com.jd.live.agent.governance.policy.live.LiveSpace;
import com.jd.live.agent.governance.policy.service.Service;
import com.jd.live.agent.governance.service.sync.Subscription;
import com.jd.live.agent.governance.service.sync.SyncKey.ServiceKey;
import com.jd.live.agent.governance.service.sync.SyncResponse;
import com.jd.live.agent.governance.service.sync.SyncStatus;
import com.jd.live.agent.governance.service.sync.http.HttpResource;
import com.jd.live.agent.governance.service.sync.http.HttpWatcher;
import com.jd.live.agent.governance.subscription.policy.PolicyWatcher;
import com.jd.live.agent.implement.event.jbus.JEventBus;
import com.jd.live.agent.implement.parser.jackson.JacksonJsonParser;
import com.jd.live.agent.implement.service.policy.multilive.config.LiveSyncConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class MultiliveSyncerTest {

    private static final LiveSyncConfig syncConfig = new LiveSyncConfig();

    private static final Application application = Application.builder()
            .name("service-provider")
            .location(Location.builder().liveSpaceId("v4bEh4kd6Jvu5QBX09qYq-qlbcs").build())
            .service(AppService.builder().name("service-provider").build())
            .build();

    private static final EventBus eventBus = new JEventBus();

    private static final JacksonJsonParser parser = new JacksonJsonParser();

    private static final Map<String, String> livespaces = new HashMap<>();

    private static final Map<String, String> services = new HashMap<>();

    @BeforeAll
    public static void setup() throws IOException {
        try (InputStream is = MultiliveSyncerTest.class.getResourceAsStream("/livespaces.json")) {
            List<LiveSpace> values = parser.read(new InputStreamReader(is), new TypeReference<List<LiveSpace>>() {
            });
            if (values != null) {
                values.forEach(value -> livespaces.put(value.getId(), parser.write(value)));
            }
        }
        try (InputStream is = MultiliveSyncerTest.class.getResourceAsStream("/microservice.json")) {
            List<Service> values = parser.read(new InputStreamReader(is), new TypeReference<List<Service>>() {
            });
            if (values != null) {
                values.forEach(value -> services.put(value.getName(), parser.write(value)));
            }
        }
    }

    @Test
    public void testLiveSpaceSyncer() {
        PolicyManager policyManager = new PolicyManager();
        LiveSpaceHttpSyncer syncer = new LiveSpaceHttpSyncer() {
            @Override
            protected HttpWatcher creatWatcher() {
                return new HttpWatcher(getType(), getSyncConfig(), application) {
                    @Override
                    protected HttpResponse<String> request(HttpResource resource) throws IOException {
                        String value = livespaces.get(resource.getId());
                        return value != null ? new HttpResponse<>(HttpStatus.OK, null, value) : new HttpResponse<>(HttpStatus.NOT_FOUND, null, null);
                    }
                };
            }

            @Override
            protected SyncResponse<LiveSpace> parseSpace(HttpLiveSpaceKey key, String config) {
                LiveSpace liveSpace = parser.read(new StringReader(config), LiveSpace.class);
                return new SyncResponse<>(SyncStatus.SUCCESS, liveSpace);
            }
        };
        syncer.setSyncConfig(syncConfig);
        syncer.setApplication(application);
        syncer.setParser(parser);
        syncer.addListener(PolicyWatcher.TYPE_LIVE_POLICY, event -> policyManager.update(null, new GovernancePolicy(CollectionUtils.singletonList((LiveSpace) event.getValue()), null, null)));
        syncer.start().join();
        Assertions.assertNotNull(policyManager.getPolicy());
        Assertions.assertNotNull(policyManager.getPolicy().getLiveSpaces());
        syncer.stop().join();
    }

    @Test
    public void testServiceSyncer() throws InterruptedException {
        Publisher<PolicySubscription> policyPublisher = eventBus.getPublisher(Publisher.POLICY_SUBSCRIBER);
        PolicyManager policyManager = PolicyManager.builder().policyPublisher(policyPublisher).application(application).build();
        TimerConfig config = new TimerConfig();
        Timer timer = new TimeScheduler("LiveAgent-timer", config.getTickTime(), config.getTicks(), config.getWorkerThreads(), config.getMaxTasks());

        LiveServiceHttpSyncer syncer = new LiveServiceHttpSyncer() {
            @Override
            protected SyncResponse<Service> getService(Subscription<ServiceKey, Service> subscription, SyncConfig config) throws IOException {
                String value = services.get(subscription.getKey().getName());
                return value != null ? new SyncResponse<>(SyncStatus.SUCCESS, parser.read(new StringReader(value), Service.class)) : new SyncResponse<>(SyncStatus.NOT_FOUND, null);
            }
        };
        syncer.setSyncConfig(syncConfig);
        syncer.setApplication(application);
        syncer.setPolicySupervisor(policyManager);
        syncer.setTimer(timer);
        syncer.setPublisher(policyPublisher);
        syncer.setParser(parser);
        CountDownLatch latch = new CountDownLatch(1);
        syncer.addListener(PolicyWatcher.TYPE_SERVICE_POLICY, event -> {
            boolean result = policyManager.update(null, new GovernancePolicy(null, CollectionUtils.singletonList((Service) event.getValue()), null));
            latch.countDown();
            return result;
        });
        policyManager.subscribe("service-provider");
        syncer.start().join();
        latch.await();
        Assertions.assertNotNull(policyManager.getPolicy());
        Assertions.assertNotNull(policyManager.getPolicy().getServices());
        syncer.stop().join();
    }

}
