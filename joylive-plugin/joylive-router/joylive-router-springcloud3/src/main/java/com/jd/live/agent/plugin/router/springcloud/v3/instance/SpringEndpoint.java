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

import com.jd.live.agent.governance.instance.AbstractEndpoint;
import com.jd.live.agent.governance.instance.EndpointState;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.DefaultResponse;
import org.springframework.cloud.client.loadbalancer.Response;

public class SpringEndpoint extends AbstractEndpoint {

    private static final String STATE_HANGUP = "hangup";
    private static final String STATE_SUSPEND = "suspend";
    private static final String LABEL_STATE = "state";

    private final ServiceInstance instance;

    private final Response<ServiceInstance> response;

    public SpringEndpoint(ServiceInstance instance) {
        this.instance = instance;
        this.response = new DefaultResponse(instance);
    }

    @Override
    public String getId() {
        String result = instance.getInstanceId();
        return result != null ? result : getAddress();
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
        return instance.getMetadata().get(key);
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

    public Response<ServiceInstance> getResponse() {
        return response;
    }
}
