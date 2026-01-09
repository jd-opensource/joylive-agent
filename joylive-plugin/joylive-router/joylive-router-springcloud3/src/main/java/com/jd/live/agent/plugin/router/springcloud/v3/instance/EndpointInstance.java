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
import com.jd.live.agent.governance.registry.ServiceEndpoint;
import com.jd.live.agent.governance.request.ServiceRequest;
import org.springframework.cloud.client.ServiceInstance;

import java.net.URI;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * A private static inner class that implements the {@link ServiceInstance} interface.
 * This class represents a specific instance of a service endpoint, providing functionality
 * to manage and interact with the instance. It is designed to be used internally within
 * its enclosing class.
 */
public class EndpointInstance implements ServiceEndpoint, ServiceInstance {

    private final ServiceEndpoint endpoint;

    public EndpointInstance(ServiceEndpoint endpoint) {
        this.endpoint = endpoint;
    }

    @Override
    public String getId() {
        return endpoint.getId();
    }

    @Override
    public String getInstanceId() {
        return endpoint.getId();
    }

    @Override
    public String getServiceId() {
        return endpoint.getService();
    }

    @Override
    public String getService() {
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

    @Override
    public Double getWeightRatio() {
        return endpoint.getWeightRatio();
    }

    @Override
    public void setWeightRatio(Double weightRatio) {
        endpoint.setWeightRatio(weightRatio);
    }

    @Override
    public Integer getWeight(ServiceRequest request) {
        return endpoint.getWeight(request);
    }

    @Override
    public int reweight(ServiceRequest request) {
        return endpoint.reweight(request);
    }

    @Override
    public String getLiveSpaceId() {
        return endpoint.getLiveSpaceId();
    }

    @Override
    public String getUnit() {
        return endpoint.getUnit();
    }

    @Override
    public String getCell() {
        return endpoint.getCell();
    }

    @Override
    public String getCloud() {
        return endpoint.getCloud();
    }

    @Override
    public String getRegion() {
        return endpoint.getRegion();
    }

    @Override
    public String getZone() {
        return endpoint.getZone();
    }

    @Override
    public String getLaneSpaceId() {
        return endpoint.getLaneSpaceId();
    }

    @Override
    public String getLane() {
        return endpoint.getLane();
    }

    @Override
    public String getGroup() {
        return endpoint.getGroup();
    }

    @Override
    public EndpointState getState() {
        return endpoint.getState();
    }

    @Override
    public <T> T getAttribute(String key) {
        return endpoint.getAttribute(key);
    }

    @Override
    public <T> T getAttributeIfAbsent(String key, Function<String, T> function) {
        return endpoint.getAttributeIfAbsent(key, function);
    }

    @Override
    public void setAttribute(String key, Object value) {
        endpoint.setAttribute(key, value);
    }

    @Override
    public <T> T removeAttribute(String key) {
        return endpoint.removeAttribute(key);
    }

    @Override
    public boolean hasAttribute(String key) {
        return endpoint.hasAttribute(key);
    }

    @Override
    public void attributes(BiConsumer<String, Object> consumer) {
        endpoint.attributes(consumer);
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
