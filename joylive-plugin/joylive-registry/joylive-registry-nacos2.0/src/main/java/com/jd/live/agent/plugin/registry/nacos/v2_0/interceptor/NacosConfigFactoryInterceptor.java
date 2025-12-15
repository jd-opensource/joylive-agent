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
package com.jd.live.agent.plugin.registry.nacos.v2_0.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.parser.ObjectParser;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.plugin.registry.nacos.v2_0.config.NacosConfigService;

/**
 * Interceptor for wrapping ConfigService with gray configuration capabilities.
 *
 * <p>This interceptor wraps the original ConfigService instance with {@link NacosConfigService}
 * to enable gray configuration functionality for Nacos configuration management.</p>
 */
public class NacosConfigFactoryInterceptor extends InterceptorAdaptor {

    private final Application application;

    private final ObjectParser json;

    public NacosConfigFactoryInterceptor(Application application, ObjectParser json) {
        this.application = application;
        this.json = json;
    }

    @Override
    public void onSuccess(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        mc.skipWithResult(new NacosConfigService(mc.getResult(), application, json));
    }
}