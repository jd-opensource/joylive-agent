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
package com.jd.live.agent.implement.service.registry.zookeeper.dubbo;

import com.jd.live.agent.governance.instance.EndpointState;
import com.jd.live.agent.governance.registry.AbstractServiceEndpoint;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * A class that represents an endpoint in the Nacos registry.
 */
@Getter
@Setter
public class DubboZookeeperEndpoint extends AbstractServiceEndpoint {

    private String scheme;

    private String host;

    private int port;

    private Map<String, String> metadata;

    public DubboZookeeperEndpoint() {
    }

    public DubboZookeeperEndpoint(String service, String group, String scheme, String host, int port, Map<String, String> metadata) {
        super(service, group);
        this.scheme = scheme;
        this.host = host;
        this.port = port;
        this.metadata = metadata;
    }

    @Override
    public EndpointState getState() {
        return EndpointState.HEALTHY;
    }

}
