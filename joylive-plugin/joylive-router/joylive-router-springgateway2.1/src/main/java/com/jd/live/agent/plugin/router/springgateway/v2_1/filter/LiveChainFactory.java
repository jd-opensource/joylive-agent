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
package com.jd.live.agent.plugin.router.springgateway.v2_1.filter;

import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.plugin.router.springgateway.v2_1.config.GatewayConfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LiveChainFactory {

    private final Map<Object, LiveChainBuilder> BUILDERS = new ConcurrentHashMap<>();

    private final InvocationContext context;

    private final GatewayConfig config;

    public LiveChainFactory(InvocationContext context, GatewayConfig config) {
        this.context = context;
        this.config = config;
    }

    /**
     * Gets or creates a LiveChainBuilder for the target.
     *
     * @param target the target object
     * @return LiveChainBuilder instance
     */
    public LiveChainBuilder create(Object target) {
        return BUILDERS.computeIfAbsent(target, t -> new LiveChainBuilder(context, config, t));
    }

}
