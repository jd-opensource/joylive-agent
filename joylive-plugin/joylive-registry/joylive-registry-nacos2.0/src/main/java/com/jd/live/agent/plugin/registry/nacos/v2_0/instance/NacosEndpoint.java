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
package com.jd.live.agent.plugin.registry.nacos.v2_0.instance;

import com.alibaba.nacos.api.naming.pojo.Instance;
import com.jd.live.agent.core.Constants;
import com.jd.live.agent.core.util.option.Converts;
import com.jd.live.agent.governance.instance.AbstractEndpoint;
import com.jd.live.agent.governance.instance.EndpointState;
import com.jd.live.agent.governance.registry.ServiceEndpoint;
import com.jd.live.agent.governance.registry.ServiceId;
import com.jd.live.agent.governance.request.ServiceRequest;

import java.util.Map;

/**
 * A class that represents an endpoint in the Nacos registry.
 */
public class NacosEndpoint extends AbstractEndpoint implements ServiceEndpoint {

    /**
     * The instance associated with this endpoint.
     */
    private final Instance instance;

    private final String service;

    /**
     * Creates a new NacosEndpoint object with the specified instance.
     *
     * @param instance the instance associated with this endpoint
     */
    public NacosEndpoint(Instance instance) {
        this(instance, null);
    }

    public NacosEndpoint(Instance instance, Boolean secure) {
        this.instance = instance;
        this.service = ServiceId.getNacosServiceName(instance.getServiceName());
        if (secure != null && secure) {
            Map<String, String> metadata = instance.getMetadata();
            if (metadata != null) {
                metadata.putIfAbsent(Constants.LABEL_SECURE, "true");
            }
        }
    }

    @Override
    public String getId() {
        return instance.getInstanceId();
    }

    @Override
    public String getService() {
        return service;
    }

    @Override
    public String getHost() {
        return instance.getIp();
    }

    @Override
    public int getPort() {
        return instance.getPort();
    }

    @Override
    public boolean isSecure() {
        return Boolean.parseBoolean(getLabel(Constants.LABEL_SECURE));
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
        if (!instance.isEnabled()) {
            return EndpointState.DISABLE;
        }
        return instance.isHealthy() ? EndpointState.HEALTHY : EndpointState.WEAK;
    }

    @Override
    public Integer getWeight(ServiceRequest request) {
        return Converts.getInteger(getLabel(Constants.LABEL_WEIGHT), (int) (instance.getWeight() * DEFAULT_WEIGHT));
    }
}
