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

import com.jd.live.agent.core.event.EventBus;
import com.jd.live.agent.core.event.Publisher;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.governance.config.SyncConfig;
import com.jd.live.agent.governance.policy.GovernancePolicy;
import com.jd.live.agent.governance.policy.PolicyManager;
import com.jd.live.agent.governance.policy.lane.LaneSpace;
import com.jd.live.agent.governance.policy.live.LiveSpace;
import com.jd.live.agent.governance.policy.service.Service;
import com.jd.live.agent.governance.subscription.policy.PolicyWatcher;
import com.jd.live.agent.implement.event.jbus.JEventBus;
import com.jd.live.agent.implement.parser.jackson.JacksonJsonParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class FileSyncerTest {

    @SuppressWarnings("unchecked")
    @Test
    public void testLiveSpaceSyncer() {
        PolicyManager policyManager = new PolicyManager();
        EventBus eventBus = new JEventBus();
        LiveSpaceFileSyncer syncer = new LiveSpaceFileSyncer();
        syncer.setSyncConfig(new SyncConfig());
        syncer.setApplication(new Application("test"));
        syncer.setPublisher(eventBus.getPublisher(Publisher.CONFIG));
        syncer.setParser(new JacksonJsonParser());
        syncer.addListener(PolicyWatcher.TYPE_LIVE_POLICY, event ->
                policyManager.update(null, new GovernancePolicy((List<LiveSpace>) event.getValue(), null, null))
        );
        syncer.start().join();
        Assertions.assertNotNull(policyManager.getPolicy());
        Assertions.assertNotNull(policyManager.getPolicy().getLiveSpaces());
        syncer.stop().join();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testServiceSyncer() {
        PolicyManager policyManager = new PolicyManager();
        EventBus eventBus = new JEventBus();
        ServiceFileSyncer syncer = new ServiceFileSyncer();
        syncer.setSyncConfig(new SyncConfig());
        syncer.setApplication(new Application("test"));
        syncer.setPublisher(eventBus.getPublisher(Publisher.CONFIG));
        syncer.setParser(new JacksonJsonParser());
        syncer.addListener(PolicyWatcher.TYPE_SERVICE_POLICY, event ->
                policyManager.update(null, new GovernancePolicy(null, (List<Service>) event.getValue(), null))
        );
        syncer.start().join();
        Assertions.assertNotNull(policyManager.getPolicy());
        Assertions.assertNotNull(policyManager.getPolicy().getServices());
        syncer.stop().join();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testLaneSpaceSyncer() {
        PolicyManager policyManager = new PolicyManager();
        EventBus eventBus = new JEventBus();
        LaneSpaceFileSyncer syncer = new LaneSpaceFileSyncer();
        syncer.setSyncConfig(new SyncConfig());
        syncer.setApplication(new Application("test"));
        syncer.setPublisher(eventBus.getPublisher(Publisher.CONFIG));
        syncer.setParser(new JacksonJsonParser());
        syncer.addListener(PolicyWatcher.TYPE_LANE_POLICY, event ->
                policyManager.update(null, new GovernancePolicy(null, null, (List<LaneSpace>) event.getValue()))
        );
        syncer.start().join();
        Assertions.assertNotNull(policyManager.getPolicy());
        Assertions.assertNotNull(policyManager.getPolicy().getLaneSpaces());
        syncer.stop().join();
    }
}
