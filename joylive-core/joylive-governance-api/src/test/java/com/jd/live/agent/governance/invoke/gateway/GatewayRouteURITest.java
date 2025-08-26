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
package com.jd.live.agent.governance.invoke.gateway;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class GatewayRouteURITest {

    @Test
    public void test() {
        GatewayRouteURI uri = new GatewayRouteURI("http://127.0.0.1:8080/test");
        Assertions.assertEquals("http", uri.getScheme());
        Assertions.assertEquals("127.0.0.1", uri.getHost());
        Assertions.assertEquals(8080, uri.getPort());
        Assertions.assertEquals("/test", uri.getUri().getPath());

        uri = new GatewayRouteURI("lb://SleepService");
        Assertions.assertEquals("lb", uri.getScheme());
        Assertions.assertEquals("SleepService", uri.getHost());

        uri = new GatewayRouteURI("lb:ws://SleepService");
        Assertions.assertEquals("lb", uri.getSchemePrefix());
        Assertions.assertEquals("ws", uri.getScheme());
        Assertions.assertEquals("SleepService", uri.getHost());

        uri = new GatewayRouteURI("lb:ws://SleepService:DEFAULT");
        Assertions.assertEquals("lb", uri.getSchemePrefix());
        Assertions.assertEquals("ws", uri.getScheme());
        Assertions.assertEquals("SleepService:DEFAULT", uri.getHost());
    }

}
