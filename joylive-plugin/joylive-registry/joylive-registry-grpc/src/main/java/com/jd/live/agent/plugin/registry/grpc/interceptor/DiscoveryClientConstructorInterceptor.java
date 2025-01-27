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
package com.jd.live.agent.plugin.registry.grpc.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.registry.Registry;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * DiscoveryClientConstructorInterceptor
 */
public class DiscoveryClientConstructorInterceptor extends InterceptorAdaptor {

    private static final Logger logger = LoggerFactory.getLogger(DiscoveryClientConstructorInterceptor.class);

    private final Registry registry;

    public DiscoveryClientConstructorInterceptor(Registry registry) {
        this.registry = registry;
    }

    @Override
    public void onSuccess(ExecutableContext ctx) {
        // gRPC is triggered on the first request
        String serviceId = ctx.getArgument(0);
        try {
            registry.subscribe(serviceId).get(5000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ignore) {
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            logger.warn("Failed to get governance policy for " + serviceId + ", caused by " + cause.getMessage(), cause);
        } catch (TimeoutException e) {
            logger.warn("Failed to get governance policy for " + serviceId + ", caused by it's timeout.");
        }
    }
}
