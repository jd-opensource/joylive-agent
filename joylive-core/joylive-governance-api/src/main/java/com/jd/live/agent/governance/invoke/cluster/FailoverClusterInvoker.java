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
package com.jd.live.agent.governance.invoke.cluster;

import com.jd.live.agent.bootstrap.exception.RejectException;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.governance.context.RequestContext;
import com.jd.live.agent.governance.context.bag.Carrier;
import com.jd.live.agent.governance.exception.RetryExhaustedException;
import com.jd.live.agent.governance.instance.Endpoint;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.invoke.retry.Retrier;
import com.jd.live.agent.governance.invoke.retry.RetrierFactory;
import com.jd.live.agent.governance.policy.service.ServicePolicy;
import com.jd.live.agent.governance.policy.service.cluster.ClusterPolicy;
import com.jd.live.agent.governance.policy.service.cluster.RetryPolicy;
import com.jd.live.agent.governance.request.ServiceRequest.OutboundRequest;
import com.jd.live.agent.governance.response.Response;
import com.jd.live.agent.governance.response.ServiceResponse.OutboundResponse;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * An implementation of {@link ClusterInvoker} that provides failover support for service requests.
 * This invoker is designed to automatically retry a failed request based on a defined {@link RetryPolicy}.
 * The failover mechanism is essential for enhancing the reliability and availability of service invocations
 * by rerouting failed requests to alternative instances within the cluster.
 */
@Extension(value = ClusterInvoker.TYPE_FAILOVER, order = ClusterInvoker.ORDER_FAILOVER)
public class FailoverClusterInvoker extends AbstractClusterInvoker {

    @Override
    public <R extends OutboundRequest,
            O extends OutboundResponse,
            E extends Endpoint,
            T extends Throwable> O execute(LiveCluster<R, O, E, T> cluster,
                                           InvocationContext context,
                                           OutboundInvocation<R> invocation,
                                           ClusterPolicy defaultPolicy) {
        AtomicInteger counter = new AtomicInteger(0);
        Supplier<O> supplier = () -> invoke(cluster, context, invocation, counter);
        ServicePolicy servicePolicy = invocation == null ? null : invocation.getServiceMetadata().getServicePolicy();
        ClusterPolicy clusterPolicy = servicePolicy == null ? null : servicePolicy.getClusterPolicy();
        RetryPolicy retryPolicy = clusterPolicy == null ? null : clusterPolicy.getRetryPolicy();
        retryPolicy = retryPolicy == null && defaultPolicy != null ? defaultPolicy.getRetryPolicy() : retryPolicy;
        if (retryPolicy != null && retryPolicy.isEnabled()) {
            RetrierFactory retrierFactory = context.getOrDefaultRetrierFactory(retryPolicy.getType());
            Retrier retrier = retrierFactory == null ? null : retrierFactory.get(retryPolicy);
            if (retrier != null) {
                Long timeout = retryPolicy.getTimeout();
                if (timeout != null && timeout > 0) {
                    RequestContext.setAttribute(Carrier.ATTRIBUTE_DEADLINE, System.currentTimeMillis() + timeout);
                } else {
                    RequestContext.removeAttribute(Carrier.ATTRIBUTE_DEADLINE);
                }
                try {
                    return retrier.execute(supplier);
                } finally {
                    RequestContext.removeAttribute(Response.KEY_LAST_EXCEPTION);
                }
            }
        }
        return supplier.get();
    }

    /**
     * Attempts to invoke the service request on an endpoint selected through the routing function.
     * This method is called internally by {@link #execute} and applies the routing and retry logic.
     *
     * @param cluster    The live cluster where the request is executed.
     * @param context    The invocation context that provides additional information and state for
     *                   the current invocation process.
     * @param invocation The specific invocation logic for the request.
     * @param counter    An atomic counter used for tracking retry attempts.
     * @return An outbound response of type {@code O} corresponding to the executed request.
     */
    @SuppressWarnings("unchecked")
    private <R extends OutboundRequest,
            O extends OutboundResponse,
            E extends Endpoint,
            T extends Throwable> O invoke(LiveCluster<R, O, E, T> cluster,
                                          InvocationContext context,
                                          OutboundInvocation<R> invocation,
                                          AtomicInteger counter) {
        R request = invocation.getRequest();
        E endpoint = null;
        try {
            List<? extends Endpoint> instances = invocation.getInstances();
            instances = counter.getAndIncrement() > 0 || instances == null || instances.isEmpty() ? cluster.route(request) : instances;
            invocation.setInstances(instances);
            List<? extends Endpoint> endpoints = context.route(invocation);
            if (endpoints != null && !endpoints.isEmpty()) {
                endpoint = (E) endpoints.get(0);
                return cluster.invoke(request, endpoint);
            }
            return cluster.createResponse(cluster.createNoProviderException(request), request, null);
        } catch (RejectException e) {
            return cluster.createResponse(cluster.createRejectException(e), request, endpoint);
        } catch (RetryExhaustedException e) {
            return cluster.createResponse(cluster.createRetryExhaustedException(e, invocation), request, endpoint);
        } catch (Throwable e) {
            return cluster.createResponse(e, request, endpoint);
        }
    }
}
