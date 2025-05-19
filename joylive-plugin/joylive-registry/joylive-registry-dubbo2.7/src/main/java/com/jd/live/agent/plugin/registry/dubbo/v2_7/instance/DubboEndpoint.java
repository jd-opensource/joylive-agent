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
import com.jd.live.agent.core.util.option.Converts;
import com.jd.live.agent.governance.instance.AbstractEndpoint;
import com.jd.live.agent.governance.instance.EndpointState;
import com.jd.live.agent.governance.registry.ServiceEndpoint;
import com.jd.live.agent.governance.request.ServiceRequest;
import org.apache.dubbo.common.URL;

import java.util.Map;

/**
 * A class that represents an endpoint in the Nacos registry.
 */
public class DubboEndpoint extends AbstractEndpoint implements ServiceEndpoint {

    public static final String KEY_GROUP = "group";

    private final URL url;

    public DubboEndpoint(URL url) {
        this.url = url;
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
    public String getLabel(String key) {
        Map<String, String> metadata = url.getParameters();
        return metadata == null ? null : metadata.get(key);
    }

    @Override
    public EndpointState getState() {
        return EndpointState.HEALTHY;
    }

    @Override
    public Integer getWeight(ServiceRequest request) {
        Double value = Converts.getDouble(Constants.LABEL_WEIGHT);
        if (value == null || value < 0) {
            return DEFAULT_WEIGHT;
        } else if (value <= 1) {
            return (int) (value * 100);
        }
        return value.intValue();
    }
}
