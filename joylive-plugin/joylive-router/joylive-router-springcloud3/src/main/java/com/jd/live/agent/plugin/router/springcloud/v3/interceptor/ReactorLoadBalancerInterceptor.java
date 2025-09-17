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
package com.jd.live.agent.plugin.router.springcloud.v3.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.core.util.cache.LazyObject;
import com.jd.live.agent.governance.context.RequestContext;
import com.jd.live.agent.governance.context.bag.Carrier;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.jd.live.agent.bootstrap.util.type.FieldAccessorFactory.getQuietly;

/**
 * ReactorLoadBalancerInterceptor
 *
 * @since 1.0.0
 */
public class ReactorLoadBalancerInterceptor extends InterceptorAdaptor {

    private static final String FIELD_SERVICE_ID = "serviceId";

    private final Map<Object, LazyObject<String>> loadBalancers = new ConcurrentHashMap<>();

    @Override
    public void onEnter(ExecutableContext ctx) {
        // Retrieves the service ID associated with the given ReactorLoadBalancer instance.
        String serviceId = loadBalancers.computeIfAbsent(ctx.getTarget(), v -> LazyObject.of((String) getQuietly(v, FIELD_SERVICE_ID))).get();
        if (serviceId != null) {
            // it's used by service instance list supplier interceptor
            RequestContext.setAttribute(Carrier.ATTRIBUTE_SERVICE_ID, serviceId);
        }
    }
}
