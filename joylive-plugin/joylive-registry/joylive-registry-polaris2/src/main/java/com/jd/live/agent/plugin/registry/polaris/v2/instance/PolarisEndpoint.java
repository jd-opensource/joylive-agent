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
package com.jd.live.agent.plugin.registry.polaris.v2.instance;

import com.jd.live.agent.governance.instance.EndpointState;
import com.jd.live.agent.governance.registry.AbstractServiceEndpoint;
import com.tencent.polaris.api.pojo.Instance;

import java.util.Map;

/**
 * A class that represents an endpoint in the Nacos registry.
 */
public class PolarisEndpoint extends AbstractServiceEndpoint {

    /**
     * The instance associated with this endpoint.
     */
    private final Instance instance;

    /**
     * Creates a new NacosEndpoint object with the specified instance.
     *
     * @param instance the instance associated with this endpoint
     */
    public PolarisEndpoint(Instance instance) {
        super(instance.getService(), null, null);
        this.instance = instance;
    }

    @Override
    public String getId() {
        return instance.getId();
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
    public Map<String, String> getMetadata() {
        return instance.getMetadata();
    }

    @Override
    public EndpointState getState() {
        if (instance.isIsolated()) {
            return EndpointState.DISABLE;
        } else if (!instance.isHealthy()) {
            return EndpointState.WEAK;
        } else {
            return EndpointState.HEALTHY;
        }
    }
}
