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
package com.jd.live.agent.governance.invoke;

import com.jd.live.agent.governance.instance.AbstractEndpoint;
import com.jd.live.agent.governance.instance.EndpointState;
import com.jd.live.agent.governance.invoke.RouteTarget.MinPercentPredicate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.*;

import static com.jd.live.agent.governance.invoke.RouteTarget.NONE_NULL;

public class RouteTargetTest {

    @Test
    void testFilter() {
        List<ServiceInstance> instances = Arrays.asList(
                new ServiceInstance("127.0.0.1", 8080).addMetadata("x-live-unit", "unit1"),
                new ServiceInstance("127.0.0.1", 9090).addMetadata("x-live-unit", "unit2"));
        RouteTarget target = RouteTarget.forward(new ArrayList<>(instances));
        Assertions.assertEquals(2, target.filter(null, 0));
        Assertions.assertEquals(2, target.filter(null, 2));
        Assertions.assertEquals(1, target.filter(null, 1));
        target = RouteTarget.forward(new ArrayList<>(instances));
        Assertions.assertEquals(1, target.filter(e -> e.isUnit("unit1")));
        // must be ArrayList
        target = RouteTarget.forward(new ArrayList<>(instances));
        Assertions.assertEquals(0, target.filter(e -> e.isUnit("unit3"), -1, NONE_NULL));
        Assertions.assertEquals(2, target.getEndpoints().size());
        Assertions.assertFalse(target.isEmpty());
        Assertions.assertEquals(0, target.filter(e -> e.isUnit("unit3"), -1, true));
        Assertions.assertTrue(target.isEmpty());
        target = RouteTarget.forward(new ArrayList<>(instances));
        Assertions.assertEquals(1, target.filter(e -> e.isUnit("unit1"), -1, new MinPercentPredicate(60)));
        Assertions.assertEquals(2, target.getEndpoints().size());
        Assertions.assertEquals(1, target.filter(e -> e.isUnit("unit1"), -1, new MinPercentPredicate(50)));

        instances = Arrays.asList(
                new ServiceInstance("127.0.0.1", 8080).addMetadata("x-live-unit", "unit1"),
                new ServiceInstance("127.0.0.1", 9090).addMetadata("x-live-unit", "unit2"),
                new ServiceInstance("127.0.0.1", 6666).addMetadata("x-live-unit", "unit1"),
                new ServiceInstance("127.0.0.1", 7777).addMetadata("x-live-unit", "unit2"),
                new ServiceInstance("127.0.0.1", 8888).addMetadata("x-live-unit", "unit1")
        );
        // must be ArrayList
        target = RouteTarget.forward(new ArrayList<>(instances));
        Assertions.assertEquals(3, target.filter(e -> e.isUnit("unit1"), -1, new MinPercentPredicate(60)));
        Assertions.assertEquals(8080, target.getEndpoints().get(0).getPort());
        Assertions.assertEquals(6666, target.getEndpoints().get(1).getPort());
        Assertions.assertEquals(8888, target.getEndpoints().get(2).getPort());
    }

    private static class ServiceInstance extends AbstractEndpoint {

        private final String host;

        private final int port;

        private Map<String, String> metadata;

        ServiceInstance(String host, int port) {
            this.host = host;
            this.port = port;
        }

        public ServiceInstance addMetadata(String key, String value) {
            if (key != null && !key.isEmpty() && value != null) {
                if (metadata == null) {
                    metadata = new HashMap<>();
                }
                metadata.put(key, value);
            }
            return this;
        }

        @Override
        public String getHost() {
            return host;
        }

        @Override
        public int getPort() {
            return port;
        }

        @Override
        public String getLabel(String key) {
            return metadata == null || key == null ? null : metadata.get(key);
        }

        @Override
        public EndpointState getState() {
            return EndpointState.HEALTHY;
        }
    }
}
