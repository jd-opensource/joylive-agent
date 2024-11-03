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
package com.jd.live.agent.plugin.router.sofarpc.instance;

import com.alipay.sofa.rpc.client.ProviderInfo;
import com.jd.live.agent.governance.instance.AbstractEndpoint;
import com.jd.live.agent.governance.instance.Endpoint;
import com.jd.live.agent.governance.instance.EndpointState;
import com.jd.live.agent.governance.request.ServiceRequest;

import java.util.function.Predicate;

/**
 * Represents an endpoint in the SOFA RPC framework, encapsulating provider information
 * and optional filtering logic.
 * <p>
 * This class implements the {@link Endpoint} interface, providing detailed information
 * about a service provider within the SOFA RPC framework. It includes functionality to
 * retrieve the provider's host, port, weight for load balancing, and custom attributes.
 * Additionally, this implementation supports specifying a predicate for further
 * filtering or selection logic among endpoints.
 * </p>
 */
public class SofaRpcEndpoint extends AbstractEndpoint {

    private final ProviderInfo provider;

    private final Predicate<ProviderInfo> predicate;

    public SofaRpcEndpoint(ProviderInfo provider) {
        this(provider, null);
    }

    public SofaRpcEndpoint(ProviderInfo provider, Predicate<ProviderInfo> predicate) {
        this.provider = provider;
        this.predicate = predicate;
    }

    public ProviderInfo getProvider() {
        return provider;
    }

    @Override
    public String getHost() {
        return provider.getHost();
    }

    @Override
    public int getPort() {
        return provider.getPort();
    }

    @Override
    public Integer getOriginWeight(ServiceRequest request) {
        return provider.getWeight();
    }

    @Override
    public String getLabel(String key) {
        return provider.getAttr(key);
    }

    @Override
    public EndpointState getState() {
        switch (provider.getStatus()) {
            case AVAILABLE:
                return EndpointState.HEALTHY;
            case BUSY:
            case DEGRADED:
                return EndpointState.WEAK;
            case WARMING_UP:
                return EndpointState.WARMUP;
            case RECOVERING:
                return EndpointState.RECOVER;
            case PAUSED:
                return EndpointState.SUSPEND;
            case PRE_CLOSE:
                return EndpointState.CLOSING;
            case DISABLED:
                return EndpointState.DISABLE;
            default:
                return EndpointState.HEALTHY;
        }

    }

    @Override
    public boolean predicate() {
        return predicate == null || predicate.test(provider);
    }
}
