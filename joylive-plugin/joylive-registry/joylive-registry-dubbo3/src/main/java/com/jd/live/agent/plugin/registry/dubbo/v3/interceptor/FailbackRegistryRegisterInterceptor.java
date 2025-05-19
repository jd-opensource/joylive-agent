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
package com.jd.live.agent.plugin.registry.dubbo.v3.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.core.Constants;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.governance.interceptor.AbstractRegistryInterceptor;
import com.jd.live.agent.governance.registry.Registry;
import com.jd.live.agent.governance.registry.ServiceInstance;
import com.jd.live.agent.governance.util.FrameworkVersion;
import org.apache.dubbo.common.URL;

import java.util.HashMap;
import java.util.Map;

/**
 * FailbackRegistryRegisterInterceptor
 */
public class FailbackRegistryRegisterInterceptor extends AbstractRegistryInterceptor {

    public FailbackRegistryRegisterInterceptor(Application application, Registry registry) {
        super(application, registry);
    }

    @Override
    protected ServiceInstance getInstance(MethodContext ctx) {
        URL url = ctx.getArgument(0);
        Map<String, String> metadata = new HashMap<>(url.getParameters());
        // application.labelRegistry(metadata::putIfAbsent);
        return ServiceInstance.builder()
                .interfaceMode(true)
                .framework(new FrameworkVersion("dubbo", url.getParameter("release", "3")))
                .service(url.getServiceInterface())
                .group(url.getParameter(Constants.LABEL_GROUP))
                .scheme(url.getProtocol())
                .host(url.getHost())
                .port(url.getPort())
                .weight(url.getParameter(Constants.LABEL_WEIGHT, 100))
                .metadata(metadata)
                .build();
    }
}
