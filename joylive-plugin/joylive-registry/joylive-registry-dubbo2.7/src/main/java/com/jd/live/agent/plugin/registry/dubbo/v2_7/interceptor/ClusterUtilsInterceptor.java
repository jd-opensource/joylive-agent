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
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;

import java.util.HashMap;
import java.util.Map;

/**
 * ClusterUtilsInterceptor
 */
public class ClusterUtilsInterceptor extends InterceptorAdaptor {

    private final Application application;

    public ClusterUtilsInterceptor(Application application) {
        this.application = application;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onEnter(ExecutableContext ctx) {
        Object[] args = ctx.getArguments();
        Map<String, String> metadata = (Map<String, String>) args[1];
        if (metadata != null && !metadata.isEmpty()) {
            Map<String, String> parameters = new HashMap<>(metadata);
            application.labelRegistry((key, value) -> parameters.remove(key));
            args[1] = parameters;
        }
    }
}
