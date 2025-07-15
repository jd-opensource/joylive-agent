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

import com.jd.live.agent.core.util.trie.PathMatcher.MatchResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PathMatcherTest {

    @Test
    public void testMatch() {
        PathMatcher<String> matcher = new PathMatcher<>();
        matcher.addPath("/", "RootHandler");
        matcher.addPath("/user", "UserHandler");
        matcher.addPath("/order/{orderId}/create", "OrderCreateHandler");
        matcher.addPath("/order/{orderId}", "OrderHandler");
        matcher.addPath("/space/{spaceId}/service/{service}", "ServiceCreateHandler");
        matcher.addPath("/space/{spaceId}/service/{service}/route/{route}", "RouteCreateHandler");
        matcher.addPath("/product/*/create", "ProductCreateHandler");
        matcher.addPath("/product/{name}/add", "ProductAddHandler");
        matcher.addPath("/product/{id}/update", "ProductUpdateHandler");
        matcher.addPath("/org/", "OrgHandler");
        matcher.addPath("/test/{a}/add", "TestAddHandler");
        matcher.addPath("/test/{a}/{b}/delete", "TestDeleteHandler");
        matcher.addPath("/test/order/update", "TestUpdateHandler");

        Assertions.assertEquals("RootHandler", matcher.match("/").getValue());
        Assertions.assertEquals("UserHandler", matcher.match("/user").getValue());
        MatchResult<String> match = matcher.match("/user/500");
        Assertions.assertEquals("UserHandler", match.getValue());
        Assertions.assertEquals(PathMatchType.PREFIX, match.getType());
        Assertions.assertEquals("OrderCreateHandler", matcher.match("/order/1/create").getValue());
        Assertions.assertEquals("OrderHandler", matcher.match("/order/2").getValue());
        Assertions.assertEquals("2", matcher.match("/order/2/update").getVariable("orderId"));
        Assertions.assertEquals("OrderHandler", matcher.match("/order/2/update").getValue());
        Assertions.assertEquals("RootHandler", matcher.match("/test").getValue());
        match = matcher.match("/space/1/service/service-consumer");
        Assertions.assertEquals("ServiceCreateHandler", match.getValue());
        Assertions.assertEquals("1", match.getVariable("spaceId"));
        Assertions.assertEquals("service-consumer", match.getVariable("service"));
        Assertions.assertEquals("ServiceCreateHandler", matcher.match("/space/1/service/service-provider").getValue());
        Assertions.assertEquals("RouteCreateHandler", matcher.match("/space/1/service/service-provider/route/abc").getValue());
        Assertions.assertEquals("abc", matcher.match("/space/1/service/service-provider/route/abc").getVariable("route"));
        Assertions.assertEquals("ServiceCreateHandler", matcher.match("/space/1/service/service-provider/lb/abc").getValue());
        Assertions.assertEquals("ProductCreateHandler", matcher.match("/product/123/create").getValue());
        Assertions.assertEquals("ProductAddHandler", matcher.match("/product/123/add").getValue());
        Assertions.assertEquals("123", matcher.match("/product/123/add").getVariable("name"));
        Assertions.assertEquals("OrgHandler", matcher.match("/org").getValue());
        Assertions.assertEquals("TestAddHandler", matcher.match("/test/order/add").getValue());
        match = matcher.match("/test/order/update");
        Assertions.assertEquals("TestUpdateHandler", match.getValue());
        Assertions.assertEquals(PathMatchType.EQUAL, match.getType());
        match = matcher.match("/test/order/1/delete");
        Assertions.assertEquals("TestDeleteHandler", match.getValue());
        Assertions.assertEquals("order", match.getVariable("a"));
        Assertions.assertEquals("1", match.getVariable("b"));
        Assertions.assertEquals(PathMatchType.EQUAL, match.getType());
        Assertions.assertEquals("RootHandler", matcher.match("/test/order/1/add").getValue());
    }
}
