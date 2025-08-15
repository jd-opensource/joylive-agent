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
package com.jd.live.agent.plugin.router.springcloud.v1.instance;

import com.jd.live.agent.governance.registry.ServiceEndpoint;
import com.netflix.loadbalancer.Server;
import lombok.Getter;

import java.util.Map;

@Getter
public class EndpointServer extends Server {

    private final Server.MetaInfo metaInfo;
    private final ServiceEndpoint endpoint;
    private final Map<String, String> metadata;

    public EndpointServer(final ServiceEndpoint endpoint) {
        super(endpoint.getHost(), endpoint.getPort());
        this.endpoint = endpoint;
        this.metaInfo = new Server.MetaInfo() {
            public String getAppName() {
                return endpoint.getService();
            }

            public String getServerGroup() {
                return null;
            }

            public String getServiceIdForDiscovery() {
                return null;
            }

            public String getInstanceId() {
                return endpoint.getId();
            }
        };
        this.metadata = endpoint.getMetadata();
    }

}
