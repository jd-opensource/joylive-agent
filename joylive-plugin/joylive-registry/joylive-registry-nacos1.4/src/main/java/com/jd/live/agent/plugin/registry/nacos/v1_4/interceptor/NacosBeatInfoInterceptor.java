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
package com.jd.live.agent.plugin.registry.nacos.v1_4.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;

/**
 * Interceptor that disables heartbeat when the application is shutting down.
 *
 * <p>Intercepts the BeatInfo.isStopped() method and returns true if the application
 * is in destroy state, effectively stopping the heartbeat mechanism during shutdown.</p>
 */
public class NacosBeatInfoInterceptor extends InterceptorAdaptor {

    private final Application application;

    public NacosBeatInfoInterceptor(Application application) {
        this.application = application;
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        if (application.getStatus().isDestroy()) {
            mc.skipWithResult(true);
        }
    }
}
