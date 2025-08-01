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

import com.jd.live.agent.core.Constants;
import com.jd.live.agent.core.util.option.Converts;
import com.jd.live.agent.governance.instance.AbstractEndpoint;
import com.jd.live.agent.governance.instance.EndpointState;
import com.jd.live.agent.governance.registry.ServiceEndpoint;
import com.jd.live.agent.governance.request.ServiceRequest;
import com.tencent.polaris.api.pojo.Instance;

import java.util.Map;

/**
 * A class that represents an endpoint in the Nacos registry.
 */
public class PolarisEndpoint extends AbstractEndpoint implements ServiceEndpoint {

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
        this.instance = instance;
    }

    @Override
    public String getId() {
        return instance.getId();
    }

    @Override
    public String getService() {
        return instance.getService();
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
    public String getLabel(String key) {
        Map<String, String> metadata = instance.getMetadata();
        return metadata == null ? null : metadata.get(key);
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

    @Override
    public Integer getWeight(ServiceRequest request) {
        return Converts.getInteger(getLabel(Constants.LABEL_WEIGHT), instance.getWeight());
    }
}
