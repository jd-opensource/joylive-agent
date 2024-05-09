package com.jd.live.agent.plugin.router.sofarpc.interceptor;

import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.invoke.InvocationContext.InvocationContextDelegate;
import com.jd.live.agent.governance.invoke.loadbalance.LoadBalancer;

/**
 * A specialized {@link InvocationContextDelegate} designed for use within the SOFA RPC environment.
 * This class overrides the {@code getLoadBalancer} method to return an instance of {@link SofaRpcLoadBalancer},
 * effectively customizing the load balancing strategy for SOFA RPC invocations.
 *
 * <p>The {@code SofaRpcInvocationContext} serves as an extension to the standard invocation context, providing
 * a mechanism to utilize a custom load balancer that is specifically tailored for handling the nuances and
 * requirements of load balancing in SOFA RPC services. This allows for enhanced control over service invocation
 * and routing, potentially improving performance, reliability, and service discovery in distributed SOFA RPC
 * environments.</p>
 *
 * @see InvocationContextDelegate
 * @see LoadBalancer
 */
class SofaRpcInvocationContext extends InvocationContextDelegate {

    SofaRpcInvocationContext(InvocationContext delegate) {
        super(delegate);
    }

    @Override
    public LoadBalancer getOrDefaultLoadBalancer(String name) {
        return new SofaRpcLoadBalancer(super.getOrDefaultLoadBalancer(name));
    }
}
