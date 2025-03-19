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
package com.jd.live.agent.plugin.router.springcloud.v2_2.instance;

import com.jd.live.agent.governance.instance.AbstractEndpoint;
import com.jd.live.agent.governance.instance.EndpointState;
import com.jd.live.agent.governance.registry.ServiceEndpoint;
import org.springframework.cloud.client.ServiceInstance;

import java.net.URI;
import java.util.Map;

/**
 * A concrete implementation of {@link AbstractEndpoint} that also implements the {@link InstanceEndpoint} interface.
 * This class provides functionality for managing and interacting with service endpoints in a Spring-based environment.
 * It combines the features of both {@link AbstractEndpoint} and {@link InstanceEndpoint} to offer a comprehensive
 * solution for handling service endpoints and instances.
 */
public class SpringEndpoint extends AbstractEndpoint implements InstanceEndpoint {

    private static final String STATE_HANGUP = "hangup";
    private static final String STATE_SUSPEND = "suspend";
    private static final String LABEL_STATE = "state";

    private final String service;

    private final ServiceInstance instance;

    public SpringEndpoint(ServiceInstance instance) {
        this.service = instance.getServiceId();
        this.instance = instance;
    }

    public SpringEndpoint(String service, ServiceInstance instance) {
        this.service = service;
        this.instance = instance;
    }

    public SpringEndpoint(String service, ServiceEndpoint endpoint) {
        this.service = service;
        this.instance = endpoint instanceof ServiceInstance ? (ServiceInstance) endpoint : new EndpointInstance(endpoint);
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
    public String getService() {
        return service;
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
    public URI getUri() {
        return instance.getUri();
    }

    @Override
    public boolean isSecure() {
        return instance.isSecure();
    }

    @Override
    public Map<String, String> getMetadata() {
        return instance.getMetadata();
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

    /**
     * A private static inner class that implements the {@link ServiceInstance} interface.
     * This class represents a specific instance of a service endpoint, providing functionality
     * to manage and interact with the instance. It is designed to be used internally within
     * its enclosing class.
     */
    private static class EndpointInstance implements ServiceInstance {
        private final ServiceEndpoint endpoint;

        EndpointInstance(ServiceEndpoint endpoint) {
            this.endpoint = endpoint;
        }

        @Override
        public String getServiceId() {
            return endpoint.getService();
        }

        @Override
        public String getHost() {
            return endpoint.getHost();
        }

        @Override
        public int getPort() {
            return endpoint.getPort();
        }

        @Override
        public boolean isSecure() {
            return endpoint.isSecure();
        }

        @Override
        public URI getUri() {
            return endpoint.getUri();
        }

        @Override
        public Map<String, String> getMetadata() {
            return endpoint.getMetadata();
        }
    }
}
