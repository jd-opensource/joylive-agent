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
package com.jd.live.agent.plugin.router.springcloud.v3.instance;

import com.jd.live.agent.governance.instance.EndpointState;
import com.jd.live.agent.governance.registry.AbstractServiceEndpoint;
import com.jd.live.agent.governance.registry.ServiceEndpoint;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.DefaultResponse;
import org.springframework.cloud.client.loadbalancer.EmptyResponse;
import org.springframework.cloud.client.loadbalancer.Response;

import java.net.URI;
import java.util.Map;

import static com.jd.live.agent.core.Constants.LABEL_STATE;
import static com.jd.live.agent.plugin.router.springcloud.v3.instance.EndpointInstance.convert;

public class SpringEndpoint extends AbstractServiceEndpoint implements ServiceInstance {

    private final ServiceInstance instance;

    public SpringEndpoint(ServiceInstance instance) {
        this(instance.getServiceId(), instance);
    }

    public SpringEndpoint(String service, ServiceInstance instance) {
        super(service, null, instance.isSecure());
        this.instance = instance;
    }

    @Override
    public String getInstanceId() {
        return instance.getInstanceId();
    }

    @Override
    public String getId() {
        String result = instance.getInstanceId();
        return result != null ? result : getAddress();
    }

    @Override
    public String getServiceId() {
        return service;
    }

    @Override
    public String getScheme() {
        return instance.getScheme();
    }

    @Override
    public URI getUri() {
        return instance.getUri();
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
        String state = getLabel(LABEL_STATE);
        if (STATE_HANGUP.equals(state)) {
            return EndpointState.DISABLE;
        } else if (STATE_SUSPEND.equals(state)) {
            return EndpointState.DISABLE;
        }
        return EndpointState.HEALTHY;
    }

    public ServiceInstance getInstance() {
        return instance;
    }

    public static Response<ServiceInstance> getResponse(ServiceEndpoint endpoint) {
        if (endpoint == null) {
            return new EmptyResponse();
        } else {
            return new DefaultResponse(convert(endpoint));
        }
    }

}
