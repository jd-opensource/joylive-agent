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
