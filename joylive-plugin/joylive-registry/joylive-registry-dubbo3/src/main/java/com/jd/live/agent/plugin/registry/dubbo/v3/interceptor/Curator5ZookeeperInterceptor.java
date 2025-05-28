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

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.core.util.time.Timer;
import com.jd.live.agent.governance.probe.HealthProbe;
import com.jd.live.agent.plugin.registry.dubbo.v3.zookeeper.CuratorFailoverClient;

/**
 * Curator5ZookeeperInterceptor
 */
public class Curator5ZookeeperInterceptor extends InterceptorAdaptor {

    private final Timer timer;

    private final HealthProbe probe;

    public Curator5ZookeeperInterceptor(Timer timer, HealthProbe probe) {
        this.timer = timer;
        this.probe = probe;
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        mc.skipWithResult(new CuratorFailoverClient(ctx.getArgument(0), timer, probe));
    }
}
