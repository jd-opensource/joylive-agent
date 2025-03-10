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
package com.jd.live.agent.plugin.registry.dubbo.v2_6.interceptor;

import com.alibaba.dubbo.config.ReferenceConfig;
import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.governance.registry.Registry;

import java.util.Map;

/**
 * ReferenceConfigInterceptor
 */
public class ReferenceConfigInterceptor extends AbstractConfigInterceptor<ReferenceConfig<?>> {

    public ReferenceConfigInterceptor(Application application, Registry registry) {
        super(application, registry);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Map<String, String> getContext(ExecutableContext ctx) {
        return (Map<String, String>) ctx.getArguments()[0];
    }

    @Override
    protected String getService(ReferenceConfig<?> config) {
        return config.getInterface();
    }

    @Override
    protected String getGroup(ReferenceConfig<?> config) {
        return config.getGroup();
    }

    @Override
    protected void subscribe(String service, String group) {
        registry.register(service, group);
    }
}
