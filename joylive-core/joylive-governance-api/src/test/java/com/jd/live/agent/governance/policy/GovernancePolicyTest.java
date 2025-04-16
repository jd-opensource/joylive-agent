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
package com.jd.live.agent.governance.policy;

import com.jd.live.agent.governance.policy.live.LiveDomain;
import com.jd.live.agent.governance.policy.live.LiveSpace;
import com.jd.live.agent.governance.policy.live.LiveSpec;
import com.jd.live.agent.governance.policy.service.Service;
import com.jd.live.agent.governance.policy.service.ServiceGroup;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

public class GovernancePolicyTest {

    @Test
    void testCaseInsensitive() {
        LiveSpace liveSpace = new LiveSpace();
        LiveSpec liveSpec = new LiveSpec();
        liveSpace.setSpec(liveSpec);
        LiveDomain domain = new LiveDomain();
        domain.setHost("www.github.com");
        liveSpec.setDomains(Arrays.asList(domain));
        List<LiveSpace> liveSpaces = Arrays.asList(liveSpace);

        Service service = new Service();
        service.setName("test");
        service.setGroups(Arrays.asList(new ServiceGroup("group1")));
        List<Service> services = Arrays.asList(service);

        GovernancePolicy policy = new GovernancePolicy(liveSpaces, services, null);

        service = policy.getService("TeST");
        Assertions.assertNotNull(service);
        Assertions.assertNotNull(service.getGroup("Group1"));
        Assertions.assertNotNull(policy.getDomain("WWW.GITHUB.COM"));

    }

}
