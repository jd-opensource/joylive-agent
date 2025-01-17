package com.jd.live.agent.plugin.router.gprc.instance;

import com.jd.live.agent.governance.instance.AbstractEndpoint;
import com.jd.live.agent.governance.instance.EndpointState;
import com.jd.live.agent.plugin.router.gprc.loadbalance.LiveRef;
import io.grpc.Attributes.Key;
import io.grpc.ConnectivityState;
import io.grpc.LoadBalancer;
import io.grpc.LoadBalancer.Subchannel;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.grpc.ConnectivityState.IDLE;

/**
 * A gRPC endpoint that provides a way to expose services over gRPC.
 */
public class GrpcEndpoint extends AbstractEndpoint {

    public static final Map<String, Key<String>> KEYS = new ConcurrentHashMap<>();

    private final Subchannel subchannel;

    private InetSocketAddress socketAddress;

    public GrpcEndpoint(Subchannel subchannel) {
        this.subchannel = subchannel;
    }

    @Override
    public String getHost() {
        return socketAddress == null ? null : socketAddress.getHostString();
    }

    @Override
    public int getPort() {
        return socketAddress == null ? 0 : socketAddress.getPort();
    }

    @Override
    public String getLabel(String key) {
        return key == null || key.isEmpty() ? null : subchannel.getAttributes().get(KEYS.computeIfAbsent(key, Key::create));
    }

    @Override
    public EndpointState getState() {
        switch (getConnectivityState()) {
            case READY:
                return EndpointState.HEALTHY;
            case IDLE:
            case CONNECTING:
            case TRANSIENT_FAILURE:
                return EndpointState.DISABLE;
            case SHUTDOWN:
            default:
                return EndpointState.CLOSING;
        }
    }

    public Subchannel getSubchannel() {
        return subchannel;
    }

    public void requestConnection() {
        subchannel.requestConnection();
    }

    public void shutdown() {
        subchannel.shutdown();
    }

    public void start(LoadBalancer.SubchannelStateListener listener) {
        subchannel.start(listener);
        socketAddress = getInetSocketAddress(subchannel);
    }

    /**
     * Gets the current ConnectivityState.
     *
     * @return the current ConnectivityState, or IDLE if no state is set
     */
    public ConnectivityState getConnectivityState() {
        LiveRef ref = subchannel.getAttributes().get(LiveRef.KEY_STATE);
        return ref == null ? IDLE : ref.getState();
    }

    /**
     * Sets the ConnectivityState to the specified newState.
     *
     * @param newState the new ConnectivityState to set
     */
    public void setConnectivityState(ConnectivityState newState) {
        LiveRef ref = subchannel.getAttributes().get(LiveRef.KEY_STATE);
        if (ref != null) {
            ref.setState(newState);
        }
    }

    private static InetSocketAddress getInetSocketAddress(Subchannel subchannel) {

        List<SocketAddress> addresses = subchannel.getAllAddresses().get(0).getAddresses();
        for (SocketAddress addr : addresses) {
            if (addr instanceof InetSocketAddress) {
                return (InetSocketAddress) addr;
            }
        }
        return null;
    }

}
