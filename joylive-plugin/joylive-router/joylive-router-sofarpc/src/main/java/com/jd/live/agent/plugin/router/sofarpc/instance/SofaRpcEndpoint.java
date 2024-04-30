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
import com.jd.live.agent.governance.instance.Endpoint;
import com.jd.live.agent.governance.instance.EndpointState;
import com.jd.live.agent.governance.request.ServiceRequest;

public class SofaRpcEndpoint implements Endpoint {

    private final ProviderInfo provider;

    public SofaRpcEndpoint(ProviderInfo provider) {
        this.provider = provider;
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
    public Integer getWeight(ServiceRequest request) {
        return Math.max(provider.getWeight(), 0);
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
}
