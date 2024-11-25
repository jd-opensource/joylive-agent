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
package com.jd.live.agent.plugin.router.gprc.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import io.grpc.Attributes;
import io.grpc.Attributes.Key;
import org.springframework.cloud.client.ServiceInstance;

import static com.jd.live.agent.core.instance.Application.label;
import static com.jd.live.agent.plugin.router.gprc.instance.GrpcEndpoint.KEYS;

/**
 * NameResolverInterceptor
 */
public class NameResolverInterceptor extends InterceptorAdaptor {

    @Override
    public void onSuccess(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        Attributes attributes = mc.getResult();
        ServiceInstance instance = mc.getArgument(0);
        Attributes.Builder builder = attributes.toBuilder();
        if (instance.getMetadata() != null) {
            label(instance.getMetadata(), (key, value) -> builder.set(KEYS.computeIfAbsent(key, Key::create), value));
        }
        attributes = builder.build();
        mc.setResult(attributes);
    }
}
