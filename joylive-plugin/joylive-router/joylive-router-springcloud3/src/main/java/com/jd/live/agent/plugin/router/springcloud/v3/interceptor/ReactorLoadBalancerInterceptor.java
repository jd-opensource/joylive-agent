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
import com.jd.live.agent.governance.context.RequestContext;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static com.jd.live.agent.bootstrap.util.type.FieldAccessorFactory.getQuietly;
import static com.jd.live.agent.governance.context.bag.Carrier.ATTRIBUTE_SERVICE_ID;

/**
 * ReactorLoadBalancerInterceptor
 *
 * @since 1.0.0
 */
public class ReactorLoadBalancerInterceptor extends InterceptorAdaptor {

    private static final String FIELD_SERVICE_ID = "serviceId";

    private final Map<Object, Optional<String>> services = new ConcurrentHashMap<>();

    @Override
    public void onEnter(ExecutableContext ctx) {
        // retrieve service id for service instance list supplier interceptor
        Optional<String> optional = services.computeIfAbsent(ctx.getTarget(), t -> Optional.ofNullable(getQuietly(t, FIELD_SERVICE_ID)));
        optional.ifPresent(serviceId -> RequestContext.setAttribute(ATTRIBUTE_SERVICE_ID, serviceId));
    }
}
