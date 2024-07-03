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
package com.jd.live.agent.core.util.trie;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PathMatcherTest {

    @Test
    public void testMatch() {
        PathMatcher<String> matcher = new PathMatcher<>();
        matcher.addPath("/", "RootHandler");
        matcher.addPath("/user", "UserHandler");
        matcher.addPath("/order/{id}/create", "OrderCreateHandler");
        matcher.addPath("/order/{id}", "OrderHandler");
        matcher.addPath("/space/{id}/service/{name}", "ServiceCreateHandler");
        matcher.addPath("/space/{id}/service/{name}/route/{name}", "RouteCreateHandler");
        matcher.addPath("/product/*/create", "ProductCreateHandler");

        Assertions.assertEquals(matcher.match("/").getValue(), "RootHandler");
        Assertions.assertEquals(matcher.match("/user").getValue(), "UserHandler");
        Assertions.assertEquals(matcher.match("/order/1/create").getValue(), "OrderCreateHandler");
        Assertions.assertEquals(matcher.match("/order/2").getValue(), "OrderHandler");
        Assertions.assertEquals(matcher.match("/order/2/update").getValue(), "OrderHandler");
        Assertions.assertEquals(matcher.match("/test").getValue(), "RootHandler");
        Assertions.assertEquals(matcher.match("/space/1/service/service-consumer").getValue(), "ServiceCreateHandler");
        Assertions.assertEquals(matcher.match("/space/1/service/service-provider").getValue(), "ServiceCreateHandler");
        Assertions.assertEquals(matcher.match("/space/1/service/service-provider/route/abc").getValue(), "RouteCreateHandler");
        Assertions.assertEquals(matcher.match("/space/1/service/service-provider/lb/abc").getValue(), "ServiceCreateHandler");
        Assertions.assertEquals(matcher.match("/product/123/create").getValue(), "ProductCreateHandler");
    }
}
