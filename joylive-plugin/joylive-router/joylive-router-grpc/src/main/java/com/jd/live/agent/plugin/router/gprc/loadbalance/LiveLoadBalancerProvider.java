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
package com.jd.live.agent.plugin.router.gprc.loadbalance;

import com.jd.live.agent.core.util.time.Timer;
import io.grpc.LoadBalancer;
import io.grpc.LoadBalancerProvider;

/**
 * A class that provides a live load balancer for distributing network traffic across multiple servers.
 */
public class LiveLoadBalancerProvider extends LoadBalancerProvider {

    private final Timer timer;

    public LiveLoadBalancerProvider(Timer timer) {
        this.timer = timer;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public int getPriority() {
        return 10;
    }

    @Override
    public String getPolicyName() {
        return "live";
    }

    @Override
    public LoadBalancer newLoadBalancer(LoadBalancer.Helper helper) {
        return new LiveLoadBalancer(helper, timer);
    }
}
