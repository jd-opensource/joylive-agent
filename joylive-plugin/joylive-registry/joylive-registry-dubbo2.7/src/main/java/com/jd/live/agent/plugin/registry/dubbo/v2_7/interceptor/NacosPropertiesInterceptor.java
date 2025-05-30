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
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;

import java.util.Properties;

import static com.jd.live.agent.governance.registry.RegistryService.KEY_SYSTEM_REGISTERED;
import static com.jd.live.agent.governance.registry.RegistryService.SYSTEM_REGISTERED;

/**
 * NacosPropertiesInterceptor
 */
public class NacosPropertiesInterceptor extends InterceptorAdaptor {

    @Override
    public void onSuccess(ExecutableContext ctx) {
        Properties properties = ((MethodContext) ctx).getResult();
        // A flag to exclude intercepting nacos.
        properties.setProperty(KEY_SYSTEM_REGISTERED, SYSTEM_REGISTERED);
    }
}
