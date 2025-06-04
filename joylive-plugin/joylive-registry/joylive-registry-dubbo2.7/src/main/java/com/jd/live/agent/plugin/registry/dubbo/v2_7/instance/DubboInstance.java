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
package com.jd.live.agent.plugin.registry.dubbo.v2_7.instance;

import com.jd.live.agent.governance.instance.EndpointState;
import org.apache.dubbo.registry.client.ServiceInstance;

import java.util.Map;

public class DubboInstance extends AbstractDubboEndpoint {

    private final ServiceInstance instance;

    public DubboInstance(ServiceInstance instance) {
        this.instance = instance;
    }

    @Override
    public String getService() {
        return instance.getServiceName();
    }

    @Override
    public Map<String, String> getMetadata() {
        return instance.getMetadata();
    }

    @Override
    public String getHost() {
        return instance.getHost();
    }

    @Override
    public int getPort() {
        return instance.getPort();
    }

    @Override
    public EndpointState getState() {
        if (!instance.isEnabled()) {
            return EndpointState.DISABLE;
        } else {
            return instance.isHealthy() ? EndpointState.HEALTHY : EndpointState.SUSPEND;
        }
    }
}
