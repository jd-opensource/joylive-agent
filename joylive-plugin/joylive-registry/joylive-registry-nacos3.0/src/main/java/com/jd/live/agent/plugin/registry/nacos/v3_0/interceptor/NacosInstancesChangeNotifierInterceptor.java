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
package com.jd.live.agent.plugin.registry.nacos.v3_0.interceptor;

import com.alibaba.nacos.client.naming.event.InstancesChangeNotifier;
import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.plugin.registry.nacos.v3_0.registry.NacosInstancePublisher;
import com.jd.live.agent.plugin.registry.nacos.v3_0.registry.NacosInstancesChangeNotifier;

import static com.jd.live.agent.plugin.registry.nacos.v3_0.registry.NacosRegistryPublisher.LOCAL_PUBLISHER;

/**
 * Nacos Instances Change Notifier Constructor Interceptor.
 *
 * <p>This interceptor injects a custom publisher from thread context
 * into the instances change notifier during construction to capture
 * instance change events. It wraps the original notifier with enhanced
 * functionality for event publishing.</p>
 */
public class NacosInstancesChangeNotifierInterceptor extends InterceptorAdaptor {

    @Override
    public void onSuccess(ExecutableContext ctx) {
        // in nacos naming service init method
        InstancesChangeNotifier notifier = (InstancesChangeNotifier) ctx.getTarget();
        NacosInstancePublisher publisher = LOCAL_PUBLISHER.get();
        if (publisher != null) {
            // avoid recursion in NacosInstancesChangeNotifier constructor
            LOCAL_PUBLISHER.remove();
            Object[] arguments = ctx.getArguments();
            String eventScope = arguments.length > 0 ? arguments[0].toString() : null;
            publisher.setNotifier(new NacosInstancesChangeNotifier(eventScope, notifier, publisher.getPublisher()));
            LOCAL_PUBLISHER.set(publisher);
        }
    }
}
