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
package com.jd.live.agent.plugin.registry.springcloud.v2_1.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.registry.Registry;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceSupplier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

/**
 * DiscoveryClientGetInterceptor
 */
public class DiscoveryClientGetInterceptor extends InterceptorAdaptor {

    private final Registry registry;

    public DiscoveryClientGetInterceptor(Registry registry) {
        this.registry = registry;
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        ServiceInstanceSupplier supplier = (ServiceInstanceSupplier) mc.getTarget();
        CompletableFuture<Void> future = registry.subscribe(supplier.getServiceId());
        if (!future.isDone() || future.isCompletedExceptionally()) {
            mc.setResult(Flux.error(HttpClientErrorException.create(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Governance policy is not synchronized.",
                    new HttpHeaders(),
                    new byte[0],
                    StandardCharsets.UTF_8)));
            mc.setSkip(true);
        }
    }
}
