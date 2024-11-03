package com.jd.live.agent.plugin.router.gprc.instance;

import com.jd.live.agent.governance.instance.AbstractEndpoint;
import com.jd.live.agent.governance.instance.EndpointState;
import io.grpc.Attributes;
import io.grpc.LoadBalancer.Subchannel;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;

/**
 * A gRPC endpoint that provides a way to expose services over gRPC.
 */
public class GrpcEndpoint extends AbstractEndpoint {

    private final Subchannel subchannel;

    private final InetSocketAddress address;

    public GrpcEndpoint(Subchannel subchannel) {
        this.subchannel = subchannel;
        this.address = getInetSocketAddress();
    }

    @Override
    public String getHost() {
        if (address != null) {
            return address.getHostString();
        }
        return null;
    }

    @Override
    public int getPort() {
        if (address != null) {
            return address.getPort();
        }
        return 0;
    }

    @Override
    public String getLabel(String key) {
        return key == null || key.isEmpty() ? null : subchannel.getAttributes().get(Attributes.Key.create(key));
    }

    @Override
    public EndpointState getState() {
        return EndpointState.HEALTHY;
    }

    private InetSocketAddress getInetSocketAddress() {
        List<SocketAddress> addresses = subchannel.getAllAddresses().get(0).getAddresses();
        for (SocketAddress addr : addresses) {
            if (addr instanceof InetSocketAddress) {
                return (InetSocketAddress) addr;
            }
        }
        return null;
    }
}
