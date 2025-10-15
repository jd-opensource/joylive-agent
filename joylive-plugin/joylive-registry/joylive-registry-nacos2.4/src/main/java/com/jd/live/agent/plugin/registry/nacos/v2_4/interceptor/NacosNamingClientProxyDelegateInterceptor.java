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
package com.jd.live.agent.plugin.registry.nacos.v2_4.interceptor;

import com.alibaba.nacos.client.naming.event.InstancesChangeNotifier;
import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.plugin.registry.nacos.v2_4.registry.NacosInstancePublisher;

import static com.jd.live.agent.plugin.registry.nacos.v2_4.registry.NacosRegistryPublisher.LOCAL_PUBLISHER;

/**
 * Nacos Naming Client Proxy Delegate Interceptor.
 *
 * <p>This interceptor modifies the notifier to use a custom notifier implementation.
 * It replaces the default InstancesChangeNotifier with a custom NacosInstancesChangeNotifier
 * during constructor execution to enable enhanced instance change handling.</p>
 */
public class NacosNamingClientProxyDelegateInterceptor extends InterceptorAdaptor {

    private static final String KEY_PUBLISHER = "publisher";

    @Override
    public void onEnter(ExecutableContext ctx) {
        Object[] arguments = ctx.getArguments();
        // Change notifier to NacosInstancesChangeNotifier
        NacosInstancePublisher publisher = LOCAL_PUBLISHER.get();
        if (publisher != null) {
            // Avoid interception at com.alibaba.nacos.client.naming.remote.gprc.NamingGrpcClientProxy.start
            // It calls NotifyCenter.registerSubscriber(this);
            LOCAL_PUBLISHER.remove();
            ctx.setAttribute(KEY_PUBLISHER, publisher);
            if (arguments.length > 3 && arguments[3] instanceof InstancesChangeNotifier) {
                arguments[3] = publisher.getNotifier();
            }
        }
    }

    @Override
    public void onSuccess(ExecutableContext ctx) {
        // rebind
        NacosInstancePublisher publisher = ctx.getAttribute(KEY_PUBLISHER);
        if (publisher != null) {
            LOCAL_PUBLISHER.set(publisher);
        }
    }
}
