package com.jd.live.agent.plugin.router.sofarpc.interceptor;

import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.jd.live.agent.governance.instance.Endpoint;
import com.jd.live.agent.governance.invoke.Invocation;
import com.jd.live.agent.governance.invoke.loadbalance.LoadBalancer;
import com.jd.live.agent.governance.invoke.loadbalance.LoadBalancer.LoadBalancerDelegate;

import java.util.List;

/**
 * A specialized {@link LoadBalancerDelegate} designed for use within the SOFA RPC framework.
 * This class overrides the {@code doSelect} method to add functionality for measuring the time taken
 * to select an endpoint from a list of available endpoints. The selection time is then recorded in the
 * {@link RpcInvokeContext} for monitoring, debugging, or other purposes.
 *
 * <p>The addition of timing logic allows for the observation and analysis of load balancing performance,
 * potentially aiding in the optimization of service discovery and request routing within a distributed
 * SOFA RPC environment. This class demonstrates a practical application of the Decorator pattern to enhance
 * or modify the behavior of an existing load balancer with minimal impact on the existing infrastructure.</p>
 *
 * @see LoadBalancerDelegate
 */
class SofaRpcLoadBalancer extends LoadBalancerDelegate {

    SofaRpcLoadBalancer(LoadBalancer delegate) {
        super(delegate);
    }

    @Override
    public <T extends Endpoint> T doSelect(List<T> endpoints, Invocation<?> invocation) {
        long loadBalanceStartTime = System.nanoTime();
        T result = super.doSelect(endpoints, invocation);
        RpcInvokeContext.getContext().put(RpcConstants.INTERNAL_KEY_CLIENT_BALANCER_TIME_NANO,
                System.nanoTime() - loadBalanceStartTime);
        return result;

    }
}
