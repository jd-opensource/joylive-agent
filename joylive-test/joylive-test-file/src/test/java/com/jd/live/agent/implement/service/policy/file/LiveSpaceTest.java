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
import com.jd.live.agent.core.extension.ExtensionManager;
import com.jd.live.agent.core.extension.jplug.JExtensionManager;
import com.jd.live.agent.core.inject.InjectSource;
import com.jd.live.agent.core.inject.Injection;
import com.jd.live.agent.core.inject.InjectorFactory;
import com.jd.live.agent.core.util.option.CascadeOption;
import com.jd.live.agent.core.util.option.Option;
import com.jd.live.agent.governance.policy.PolicyManager;
import com.jd.live.agent.governance.policy.PolicySupervisor;
import com.jd.live.agent.governance.policy.PolicySupplier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

public class LiveSpaceTest {

    @Test
    public void testSync() {
        ExtensionManager extensionManager = new JExtensionManager();
        Injection injectors = extensionManager.getOrLoadExtensible(InjectorFactory.class).getExtension().create(extensionManager);
        PolicyManager policyManager = new PolicyManager();
        Map<String, Object> map = new HashMap<>();
        Map<String, Object> agent = new HashMap<>();
        Map<String, Object> policy = new HashMap<>();
        Map<String, Object> liveSpace = new HashMap<>();
        liveSpace.put("interval", 3000);
        policy.put("liveSpace", liveSpace);
        agent.put("sync", policy);
        map.put("agent", agent);
        Option option = new CascadeOption(map);
        Map<String, Object> components = new HashMap<>();
        components.put(PolicySupervisor.COMPONENT_POLICY_SUPERVISOR, policyManager);
        components.put(PolicySupplier.COMPONENT_POLICY_SUPPLIER, policyManager);
        components.put(ExtensionManager.COMPONENT_EXTENSION_MANAGER, extensionManager);
        components.put(EventBus.COMPONENT_EVENT_BUS, extensionManager.getOrLoadExtension(EventBus.class));
        InjectSource ctx = new InjectSource(option, components);
        LiveSpaceFileSyncer service = new LiveSpaceFileSyncer();
        injectors.inject(ctx, service);
        Assertions.assertNotNull(service.getSyncConfig());
        Assertions.assertEquals(3000, service.getSyncConfig().getInterval());
        service.start().join();
        Assertions.assertNotNull(policyManager.getPolicy());
        service.stop().join();
    }
}
