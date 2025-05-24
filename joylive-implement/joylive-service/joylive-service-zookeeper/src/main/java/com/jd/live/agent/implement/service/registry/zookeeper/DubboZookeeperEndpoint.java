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
package com.jd.live.agent.implement.service.registry.zookeeper;

import com.jd.live.agent.core.Constants;
import com.jd.live.agent.core.util.option.Converts;
import com.jd.live.agent.governance.instance.AbstractEndpoint;
import com.jd.live.agent.governance.instance.EndpointState;
import com.jd.live.agent.governance.registry.ServiceEndpoint;
import com.jd.live.agent.governance.request.ServiceRequest;
import lombok.*;

import java.util.Map;

/**
 * A class that represents an endpoint in the Nacos registry.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DubboZookeeperEndpoint extends AbstractEndpoint implements ServiceEndpoint {

    private String scheme;

    private String service;

    private String group;

    private String host;

    private int port;

    private Map<String, String> metadata;

    @Override
    public boolean isSecure() {
        return Boolean.parseBoolean(getLabel(Constants.LABEL_SECURE));
    }

    @Override
    public String getLabel(String key) {
        return metadata == null ? null : metadata.get(key);
    }

    @Override
    public EndpointState getState() {
        return EndpointState.HEALTHY;
    }

    @Override
    public Integer getWeight(ServiceRequest request) {
        return Converts.getInteger(getLabel(Constants.LABEL_WEIGHT));
    }
}
