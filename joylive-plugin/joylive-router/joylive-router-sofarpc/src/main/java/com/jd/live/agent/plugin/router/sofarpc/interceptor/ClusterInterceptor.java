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
package com.jd.live.agent.plugin.router.sofarpc.interceptor;

import com.alipay.sofa.rpc.client.AbstractCluster;
import com.alipay.sofa.rpc.client.LiveCluster;
import com.alipay.sofa.rpc.client.ProviderInfo;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.rpc.core.exception.RpcErrorType;
import com.alipay.sofa.rpc.core.exception.SofaRouteException;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.log.LogCodes;
import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.bootstrap.exception.RejectException;
import com.jd.live.agent.governance.instance.Endpoint;
import com.jd.live.agent.governance.interceptor.AbstractInterceptor.AbstractRouteInterceptor;
import com.jd.live.agent.governance.invoke.Invocation;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.invoke.filter.RouteFilter;
import com.jd.live.agent.governance.invoke.loadbalance.LoadBalancer;
import com.jd.live.agent.governance.response.Response;
import com.jd.live.agent.plugin.router.sofarpc.instance.SofaRpcEndpoint;
import com.jd.live.agent.plugin.router.sofarpc.request.SofaRpcRequest.SofaRpcOutboundRequest;
import com.jd.live.agent.plugin.router.sofarpc.request.invoke.SofaRpcInvocation.SofaRpcOutboundInvocation;
import com.jd.live.agent.plugin.router.sofarpc.response.SofaRpcResponse.SofaRpcOutboundResponse;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.alipay.sofa.rpc.core.exception.RpcErrorType.CLIENT_ROUTER;

/**
 * ClusterInterceptor
 */
public class ClusterInterceptor extends AbstractRouteInterceptor<SofaRpcOutboundRequest, SofaRpcOutboundInvocation> {

    private final Map<AbstractCluster, LiveCluster> clusters = new ConcurrentHashMap<>();

    public ClusterInterceptor(InvocationContext context, List<RouteFilter> filters) {
        super(context, filters);
    }

    /**
     * Enhanced logic after method execution<br>
     * <p>
     *
     * @param ctx ExecutableContext
     */
    @Override
    public void onEnter(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        SofaRequest request = (SofaRequest) ctx.getArguments()[0];
        LiveCluster cluster = clusters.computeIfAbsent((AbstractCluster) ctx.getTarget(), LiveCluster::new);
        SofaRpcOutboundRequest outboundRequest = new SofaRpcOutboundRequest(request, cluster);
        SofaRpcOutboundInvocation invocation = createOutlet(outboundRequest);
        Response response = invokeWithRetry(invocation, () -> invoke(request, invocation, cluster));
        if (response.getThrowable() != null) {
            mc.setThrowable(response.getThrowable());
        } else {
            mc.setResult(response.getResponse());
        }
        mc.setSkip(true);
    }

    /**
     * Invokes a SOFA RPC call using the provided request and invocation context. This method attempts to route the
     * request to one of the available SOFA RPC endpoints and returns a response wrapped in a {@link SofaRpcOutboundResponse}.
     * <p>
     * The routing process is determined by the invocation's routing logic, which selects appropriate endpoints from
     * the provided instances. If the routing is successful and an endpoint is available, the method proceeds with
     * executing the request through the cluster's filter chain. In case of any RPC-related exceptions, these are
     * captured and wrapped in the outbound response.
     * </p>
     * <p>
     * If no endpoints are available or the routing cannot be completed, the method returns a {@link SofaRpcOutboundResponse}
     * with an appropriate exception indicating the failure to find an available provider.
     * </p>
     *
     * @param request    The {@link SofaRequest} representing the RPC call to be made.
     * @param invocation The {@link SofaRpcOutboundInvocation} context holding invocation details.
     * @param cluster    The {@link LiveCluster} through which the request is to be executed.
     * @return A {@link SofaRpcOutboundResponse} representing the outcome of the invocation. This response may
     * contain the result of the RPC call or an exception if the call was unsuccessful or no endpoints could be found.
     * @throws RejectException If the invocation is rejected by the routing logic or cluster configuration.
     */
    private SofaRpcOutboundResponse invoke(SofaRequest request,
                                           SofaRpcOutboundInvocation invocation,
                                           LiveCluster cluster) {
        try {
            List<ProviderInfo> invokers = cluster.route(request);
            List<SofaRpcEndpoint> instances = invokers.stream().map(e -> new SofaRpcEndpoint(e, cluster::isConnected)).collect(Collectors.toList());
            invocation.setInstances(instances);
            List<? extends Endpoint> endpoints = routing(invocation);
            if (endpoints != null && !endpoints.isEmpty()) {
                try {
                    SofaResponse sofaResponse = cluster.invoke(((SofaRpcEndpoint) endpoints.get(0)).getProvider(), request);
                    return new SofaRpcOutboundResponse(sofaResponse);
                } catch (SofaRpcException e) {
                    return new SofaRpcOutboundResponse(e, this::isRetryable);
                }
            }
            return new SofaRpcOutboundResponse(new SofaRouteException(
                    LogCodes.getLog(LogCodes.ERROR_NO_AVAILABLE_PROVIDER,
                            request.getTargetServiceUniqueName(), "[]")), this::isRetryable);
        } catch (RejectException e) {
            return new SofaRpcOutboundResponse(new SofaRpcException(CLIENT_ROUTER, e.getMessage()), this::isRetryable);
        }
    }

    @Override
    protected SofaRpcOutboundInvocation createOutlet(SofaRpcOutboundRequest request) {
        return new SofaRpcOutboundInvocation(request, new SofaRpcInvocationContext(context));
    }

    /**
     * Determines if the provided response is retryable based on the nature of the response
     * and the type of exception it contains.
     *
     * This method considers a response retryable under two main conditions:
     * 1. The response object is null, indicating that no successful response was received.
     * 2. The throwable within the response is an instance of {@code SofaRpcException} with
     *    specific error types such as SERVER_BUSY or CLIENT_TIMEOUT, which are typically
     *    transient errors and might be resolved upon retrying.
     *
     * @param response The RPC response to evaluate for retryability.
     * @return {@code true} if the response is considered retryable; {@code false} otherwise.
     */
    private boolean isRetryable(Response response) {
        if (response.getResponse() == null) {
            return true;
        } else if (!(response.getThrowable() instanceof SofaRpcException)) {
            return false;
        }
        switch (((SofaRpcException) response.getThrowable()).getErrorType()) {
            case RpcErrorType.SERVER_BUSY:
            case RpcErrorType.CLIENT_TIMEOUT:
                return true;
            default:
                return false;
        }
    }

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
    private static class SofaRpcInvocationContext extends InvocationContext.InvocationContextDelegate {

        public SofaRpcInvocationContext(InvocationContext delegate) {
            super(delegate);
        }

        @Override
        public LoadBalancer getOrDefaultLoadBalancer(String name) {
            return new SofaRpcLoadBalancer(super.getOrDefaultLoadBalancer(name));
        }
    }

    /**
     * A specialized {@link LoadBalancer.LoadBalancerDelegate} designed for use within the SOFA RPC framework.
     * This class overrides the {@code doSelect} method to add functionality for measuring the time taken
     * to select an endpoint from a list of available endpoints. The selection time is then recorded in the
     * {@link RpcInvokeContext} for monitoring, debugging, or other purposes.
     *
     * <p>The addition of timing logic allows for the observation and analysis of load balancing performance,
     * potentially aiding in the optimization of service discovery and request routing within a distributed
     * SOFA RPC environment. This class demonstrates a practical application of the Decorator pattern to enhance
     * or modify the behavior of an existing load balancer with minimal impact on the existing infrastructure.</p>
     *
     * @see LoadBalancer.LoadBalancerDelegate
     */
    private static class SofaRpcLoadBalancer extends LoadBalancer.LoadBalancerDelegate {

        public SofaRpcLoadBalancer(LoadBalancer delegate) {
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
}
