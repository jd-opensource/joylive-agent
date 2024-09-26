package com.jd.live.agent.plugin.router.gprc.instance;

import com.jd.live.agent.governance.instance.AbstractEndpoint;
import com.jd.live.agent.governance.instance.EndpointState;
import com.jd.live.agent.governance.request.ServiceRequest;
import net.devh.boot.grpc.client.config.GrpcChannelProperties;

public class GRpcEndpoint extends AbstractEndpoint {

    private final GrpcChannelProperties properties;

    public GRpcEndpoint(GrpcChannelProperties properties) {
        this.properties = properties;
    }

    @Override
    protected int computeWeight(ServiceRequest request) {
        return 0;
    }

    @Override
    public String getHost() {
        return properties.getAddress().getHost();
    }

    @Override
    public int getPort() {
        return properties.getAddress().getPort();
    }

    @Override
    public String getLabel(String key) {
        return "";
    }

    @Override
    public EndpointState getState() {
        return null;
    }
}
