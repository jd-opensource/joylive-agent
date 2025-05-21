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
package com.jd.live.agent.plugin.registry.dubbo.v2_7.instance;

import com.jd.live.agent.core.Constants;
import com.jd.live.agent.governance.instance.EndpointState;
import com.jd.live.agent.governance.registry.ServiceEndpoint;
import com.jd.live.agent.governance.registry.ServiceId;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.registry.client.InstanceAddressURL;
import org.apache.dubbo.registry.client.ServiceInstance;

import java.util.Map;

import static com.jd.live.agent.plugin.registry.dubbo.v2_7.util.UrlUtils.toServiceId;

/**
 * A class that represents an endpoint in the Nacos registry.
 */
public class DubboEndpoint extends AbstractDubboEndpoint implements ServiceEndpoint {

    private final URL url;

    public DubboEndpoint(URL url) {
        this.url = url;
        ServiceId serviceId = toServiceId(url);
        this.service = serviceId.getService();
        this.group = serviceId.getGroup();
    }

    @Override
    public String getId() {
        int port = url.getPort();
        String host = url.getHost();
        return port <= 0 ? host : host + ":" + port;
    }

    @Override
    public String getService() {
        return url.getServiceInterface();
    }

    @Override
    public String getHost() {
        return url.getHost();
    }

    @Override
    public int getPort() {
        return url.getPort();
    }

    @Override
    public boolean isSecure() {
        return Boolean.parseBoolean(getLabel(Constants.LABEL_SECURE));
    }

    @Override
    public Map<String, String> getMetadata() {
        return url.getParameters();
    }

    @Override
    public EndpointState getState() {
        if (url instanceof InstanceAddressURL) {
            InstanceAddressURL iau = (InstanceAddressURL) url;
            ServiceInstance instance = iau.getInstance();
            if (!instance.isEnabled()) {
                return EndpointState.DISABLE;
            } else {
                return instance.isHealthy() ? EndpointState.HEALTHY : EndpointState.SUSPEND;
            }
        }
        return super.getState();
    }
}
