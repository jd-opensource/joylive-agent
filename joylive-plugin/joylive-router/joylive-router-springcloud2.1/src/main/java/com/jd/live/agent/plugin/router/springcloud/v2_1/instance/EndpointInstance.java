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
package com.jd.live.agent.plugin.router.springcloud.v2_1.instance;

import com.jd.live.agent.governance.registry.ServiceEndpoint;
import org.springframework.cloud.client.ServiceInstance;

import java.net.URI;
import java.util.Map;

/**
 * A class that implements the {@link ServiceInstance} interface.
 */
public class EndpointInstance implements ServiceInstance {

    private final ServiceEndpoint endpoint;

    public EndpointInstance(ServiceEndpoint endpoint) {
        this.endpoint = endpoint;
    }

    @Override
    public String getServiceId() {
        return endpoint.getService();
    }

    @Override
    public String getScheme() {
        return endpoint.getScheme();
    }

    @Override
    public boolean isSecure() {
        return endpoint.isSecure();
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
    public URI getUri() {
        return endpoint.getUri();
    }

    @Override
    public Map<String, String> getMetadata() {
        return endpoint.getMetadata();
    }

    /**
     * Converts a {@link ServiceEndpoint} to a {@link ServiceInstance}.
     *
     * @param endpoint the endpoint to convert (may be {@code null})
     * @return the converted instance, or {@code null} if input was {@code null}
     */
    public static ServiceInstance convert(ServiceEndpoint endpoint) {
        if (endpoint == null) {
            return null;
        } else if (endpoint instanceof ServiceInstance) {
            return (ServiceInstance) endpoint;
        } else {
            return new EndpointInstance(endpoint);
        }
    }
}
