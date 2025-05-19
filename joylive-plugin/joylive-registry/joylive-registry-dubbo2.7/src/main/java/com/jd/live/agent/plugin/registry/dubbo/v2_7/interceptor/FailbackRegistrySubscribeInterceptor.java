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
package com.jd.live.agent.plugin.registry.dubbo.v2_7.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.plugin.registry.dubbo.v2_7.registry.DubboExecutorService;
import com.jd.live.agent.plugin.registry.dubbo.v2_7.registry.DubboNotifyListener;
import com.jd.live.agent.plugin.registry.dubbo.v2_7.registry.DubboRegistryPublisher;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.registry.NotifyListener;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import static com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessorFactory.getQuietly;
import static com.jd.live.agent.plugin.registry.dubbo.v2_7.registry.DubboExecutorService.FIELD_REGISTRY_CACHE_EXECUTOR;
import static org.apache.dubbo.registry.Constants.CONSUMER_PROTOCOL;

/**
 * FailbackRegistrySubscribeInterceptor
 */
public class FailbackRegistrySubscribeInterceptor extends InterceptorAdaptor {

    private static final Map<String, String> GROUPS = new HashMap<String, String>() {{
        put("org.apache.dubbo.registry.nacos.NacosRegistry", "DEFAULT_GROUP");
    }};

    @Override
    public void onEnter(ExecutableContext ctx) {
        Object target = ctx.getTarget();
        Object[] arguments = ctx.getArguments();
        URL url = (URL) arguments[0];
        NotifyListener listener = (NotifyListener) arguments[1];
        if (CONSUMER_PROTOCOL.equals(url.getProtocol())) {
            ExecutorService executor = getQuietly(target, FIELD_REGISTRY_CACHE_EXECUTOR);
            if (executor instanceof DubboExecutorService) {
                DubboExecutorService des = (DubboExecutorService) executor;
                DubboRegistryPublisher publisher = des.getPublisher();
                publisher.subscribe(url);
                arguments[1] = new DubboNotifyListener(url, listener, publisher, GROUPS.get(target.getClass().getName()));
            }
        }
    }
}
