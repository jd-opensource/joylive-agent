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
package com.jd.live.agent.plugin.registry.springcloud.v2_2.instance;

import com.jd.live.agent.governance.instance.AbstractEndpoint;
import com.jd.live.agent.governance.instance.EndpointState;
import com.jd.live.agent.governance.registry.ServiceEndpoint;
import org.springframework.cloud.client.ServiceInstance;

import java.net.URI;
import java.util.Map;

import static com.jd.live.agent.core.Constants.LABEL_STATE;

public class SpringEndpoint extends AbstractEndpoint implements ServiceEndpoint, ServiceInstance {

    private final ServiceInstance instance;

    public SpringEndpoint(ServiceInstance instance) {
        this.instance = instance;
    }

    @Override
    public String getId() {
        String result = instance.getInstanceId();
        return result != null ? result : getAddress();
    }

    @Override
    public String getInstanceId() {
        return instance.getInstanceId();
    }

    @Override
    public String getScheme() {
        return instance.getScheme();
    }

    @Override
    public String getServiceId() {
        return instance.getServiceId();
    }

    @Override
    public String getService() {
        return instance.getServiceId();
    }

    @Override
    public URI getUri() {
        return instance.getUri();
    }

    @Override
    public boolean isSecure() {
        return instance.isSecure();
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

    @Override
    public Map<String, String> getMetadata() {
        return instance.getMetadata();
    }

    public ServiceInstance getInstance() {
        return instance;
    }

}
