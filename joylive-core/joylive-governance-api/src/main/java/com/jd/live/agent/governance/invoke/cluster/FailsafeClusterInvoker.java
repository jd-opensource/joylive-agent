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

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.governance.instance.Endpoint;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.policy.service.cluster.ClusterPolicy;
import com.jd.live.agent.governance.request.ServiceRequest.OutboundRequest;
import com.jd.live.agent.governance.exception.ServiceError;
import com.jd.live.agent.governance.response.ServiceResponse.OutboundResponse;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * A {@code ClusterInvoker} that implements the failsafe (or fail-silent) invocation strategy.
 * <p>
 * The failsafe strategy is designed to handle errors and exceptions by logging them and continuing
 * operation, rather than propagating the error and potentially causing the caller or system to fail.
 * This approach is useful in systems where availability and resilience are more critical than the
 * immediate correctness of every operation. In the event of an error during invocation, this invoker
 * logs the error and returns a default or null response, effectively "swallowing" the error to
 * maintain system stability.
 * </p>
 */
@Extension(value = ClusterInvoker.TYPE_FAILSAFE, order = ClusterInvoker.ORDER_FAILSAFE)
public class FailsafeClusterInvoker extends AbstractClusterInvoker {

    private static final Logger logger = LoggerFactory.getLogger(FailsafeClusterInvoker.class);

    @Override
    public <R extends OutboundRequest,
            O extends OutboundResponse,
            E extends Endpoint> CompletionStage<O> execute(LiveCluster<R, O, E> cluster,
                                                            OutboundInvocation<R> invocation,
                                                            ClusterPolicy defaultPolicy) {
        return invoke(cluster, invocation, 0);
    }

    @Override
    protected <R extends OutboundRequest,
            O extends OutboundResponse,
            E extends Endpoint> void onException(LiveCluster<R, O, E> cluster,
                                                  OutboundInvocation<R> invocation,
                                                  O response,
                                                  ServiceError error,
                                                  E endpoint,
                                                  CompletableFuture<O> result) {
        logger.error("Failsafe ignore exception: " + error.getError());
        R request = invocation.getRequest();
        invocation.onFailure(endpoint, error.getThrowable());
        response = cluster.createResponse(null, request, null);
        cluster.onSuccess(response, request, endpoint);
        result.complete(response);
    }
}
