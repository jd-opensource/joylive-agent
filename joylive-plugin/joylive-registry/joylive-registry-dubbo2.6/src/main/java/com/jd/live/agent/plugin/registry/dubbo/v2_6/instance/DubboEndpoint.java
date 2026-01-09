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
package com.jd.live.agent.plugin.registry.dubbo.v2_6.instance;

import com.alibaba.dubbo.common.URL;
import com.jd.live.agent.governance.instance.EndpointState;
import com.jd.live.agent.governance.registry.AbstractServiceEndpoint;
import com.jd.live.agent.governance.registry.ServiceId;

import java.util.Map;

import static com.jd.live.agent.plugin.registry.dubbo.v2_6.util.UrlUtils.toServiceId;

/**
 * A class that represents an endpoint in the Nacos registry.
 */
public class DubboEndpoint extends AbstractServiceEndpoint {

    private final URL url;

    public DubboEndpoint(URL url) {
        this(url, toServiceId(url));
    }

    public DubboEndpoint(URL url, ServiceId serviceId) {
        super(serviceId.getService(), serviceId.getGroup(), null);
        this.url = url;
    }

    @Override
    public String getId() {
        int port = url.getPort();
        String host = url.getHost();
        return port <= 0 ? host : host + ":" + port;
    }

    @Override
    public String getScheme() {
        return "dubbo";
    }

    @Override
    public EndpointState getState() {
        return EndpointState.HEALTHY;
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
    public Map<String, String> getMetadata() {
        return url.getParameters();
    }
}
