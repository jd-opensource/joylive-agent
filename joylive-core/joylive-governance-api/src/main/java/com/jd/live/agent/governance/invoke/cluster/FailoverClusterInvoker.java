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
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.governance.exception.RetryException.RetryExhaustedException;
import com.jd.live.agent.governance.exception.RetryException.RetryTimeoutException;
import com.jd.live.agent.governance.instance.Endpoint;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.policy.service.ServicePolicy;
import com.jd.live.agent.governance.policy.service.cluster.ClusterPolicy;
import com.jd.live.agent.governance.policy.service.cluster.RetryPolicy;
import com.jd.live.agent.governance.request.ServiceRequest.OutboundRequest;
import com.jd.live.agent.governance.response.ServiceResponse.OutboundResponse;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * An implementation of {@link ClusterInvoker} that provides failover support for service requests.
 * This invoker is designed to automatically retry a failed request based on a defined {@link RetryPolicy}.
 * The failover mechanism is essential for enhancing the reliability and availability of service invocations
 * by rerouting failed requests to alternative instances within the cluster.
 */
@Injectable
@Extension(value = ClusterInvoker.TYPE_FAILOVER, order = ClusterInvoker.ORDER_FAILOVER)
public class FailoverClusterInvoker extends AbstractClusterInvoker {

    @Override
    public <R extends OutboundRequest,
            O extends OutboundResponse,
            E extends Endpoint,
            T extends Throwable> CompletionStage<O> execute(LiveCluster<R, O, E, T> cluster,
                                                            OutboundInvocation<R> invocation,
                                                            ClusterPolicy defaultPolicy) {
        ServicePolicy servicePolicy = invocation.getServiceMetadata().getServicePolicy();
        ClusterPolicy clusterPolicy = servicePolicy == null ? null : servicePolicy.getClusterPolicy();
        RetryPolicy retryPolicy = clusterPolicy == null ? null : clusterPolicy.getRetryPolicy();
        retryPolicy = retryPolicy == null && defaultPolicy != null ? defaultPolicy.getRetryPolicy() : retryPolicy;
        RetryContext<R, O, E, T> retryContext = new RetryContext<>(retryPolicy, cluster);
        Supplier<CompletionStage<O>> supplier = () -> invoke(cluster, invocation, retryContext.getCount());
        cluster.onStart(invocation.getRequest());
        return retryContext.execute(invocation.getRequest(), supplier).exceptionally(e -> {
            Throwable throwable = e instanceof RetryExhaustedException
                    ? cluster.createRetryExhaustedException((RetryExhaustedException) e, invocation)
                    : e;
            return cluster.createResponse(throwable, invocation.getRequest(), null);
        });
    }

    /**
     * A context class designed to manage retry operations for outbound responses.
     * <p>
     * This class encapsulates the logic for executing operations with retry capabilities,
     * governed by a specified {@link RetryPolicy}. It supports operations that return a
     * {@link OutboundResponse} and handles retries based on the policy's criteria, which
     * may include retry counts, deadlines, and specific conditions for retrying based on
     * exceptions or response codes.
     * </p>
     *
     * @param <R> The type of the outbound request that extends {@link OutboundRequest}.
     * @param <O> The type of the outbound response that extends {@link OutboundResponse}.
     */
    private static class RetryContext<R extends OutboundRequest,
            O extends OutboundResponse,
            E extends Endpoint,
            T extends Throwable> {

        /**
         * The retry policy defining the rules for retrying the operation.
         */
        private final RetryPolicy retryPolicy;

        /**
         * The cluster managing the distribution and processing of the request
         */
        private final LiveCluster<R, O, E, T> cluster;

        /**
         * A counter tracking the number of retry attempts made.
         */
        private final AtomicInteger counter;

        /**
         * The deadline timestamp in milliseconds by which the operation should complete.
         */
        private final long deadline;

        /**
         * Constructs a new {@code RetryContext} with the specified retry policy and response function.
         *
         * @param retryPolicy The {@link RetryPolicy} to govern retry behavior.
         * @param cluster     The {@link LiveCluster} managing the distribution and processing of the request
         */
        RetryContext(RetryPolicy retryPolicy, LiveCluster<R, O, E, T> cluster) {
            this.retryPolicy = retryPolicy;
            this.cluster = cluster;
            this.counter = new AtomicInteger(0);
            this.deadline = retryPolicy == null ? 0 : retryPolicy.getDeadline(System.currentTimeMillis());
        }

        /**
         * Initiates the execution of the operation with retry logic.
         * <p>
         * This method attempts to execute the provided operation. If the operation fails and
         * meets the criteria for retrying as defined by the {@link RetryPolicy}, it will be
         * retried until the policy's conditions are no longer met.
         * </p>
         *
         * @param request  The request that was being processed.
         * @param supplier A supplier providing the operation to be executed as a {@link CompletionStage}.
         * @return A {@link CompletionStage} representing the eventual completion of the operation,
         * either successfully or with an error.
         */
        public CompletionStage<O> execute(R request, Supplier<CompletionStage<O>> supplier) {
            CompletableFuture<O> result = new CompletableFuture<>();
            doExecute(request, supplier, result);
            return result;

        }

        /**
         * Recursively executes the operation, applying retry logic and completing the future
         * based on the outcome of each attempt.
         * <p>
         * This method is called internally to handle the actual execution and retry logic.
         * It uses the {@link RetryPolicy} to determine whether an operation should be retried
         * in case of failure or certain response conditions.
         * </p>
         *
         * @param request  The request that was being processed.
         * @param supplier A supplier providing the operation to be executed.
         * @param future   The {@link CompletableFuture} to be completed with the operation's result.
         */
        private void doExecute(R request, Supplier<CompletionStage<O>> supplier, CompletableFuture<O> future) {
            int count = counter.getAndIncrement();
            cluster.onRetry(count);
            CompletionStage<O> stage = supplier.get();
            stage.whenComplete((v, e) -> {
                switch (isRetryable(request, v, e, count)) {
                    case RETRY:
                        T destroyedException = cluster.isDestroyed() ? cluster.createUnReadyException(request) : null;
                        if (destroyedException != null) {
                            future.completeExceptionally(destroyedException);
                        } else {
                            doExecute(request, supplier, future);
                        }
                        break;
                    case EXHAUSTED:
                        future.completeExceptionally(new RetryExhaustedException("max retries is reached out.", v.getThrowable(), retryPolicy.getRetry()));
                        break;
                    case TIMEOUT:
                        future.completeExceptionally(new RetryTimeoutException("retry is timeout.", v.getThrowable(), retryPolicy.getTimeout()));
                        break;
                    default:
                        if (e != null) {
                            future.completeExceptionally(e);
                        } else {
                            future.complete(v);
                        }
                }
            });
        }

        private int getCount() {
            return counter.get();
        }

        /**
         * Determines whether the operation should be retried based on the response, exception encountered,
         * and the retry policy.
         *
         * @param request  The request that was being processed.
         * @param response The response from the operation, which may be null in case of an exception.
         * @param e        The exception encountered during the operation, which may be null if the operation succeeded.
         * @param count    The retry counter.
         * @return {@code true} if the operation should be retried, {@code false} otherwise.
         */
        private RetryType isRetryable(R request, O response, Throwable e, int count) {
            // TODO limit retryable methods. such as HttpMethod.GET
            if (retryPolicy == null || !retryPolicy.isEnabled()) {
                return RetryType.NONE;
            } else if (deadline > 0 && System.currentTimeMillis() > deadline) {
                return RetryType.TIMEOUT;
            } else if (count >= retryPolicy.getRetry()) {
                return RetryType.EXHAUSTED;
            } else if (retryPolicy.isRetry(e)) {
                return RetryType.RETRY;
            } else if (e instanceof RejectException) {
                return RetryType.NONE;
            } else {
                return response != null && (
                        retryPolicy.isRetry(response.getThrowable())
                                || retryPolicy.isRetry(response.getCode())
                                || response.isRetryable())
                        ? RetryType.RETRY
                        : RetryType.NONE;
            }
        }
    }

    /**
     * Defines the types of retry behavior that can be encountered during operations that support retry logic.
     * This enumeration is used to categorize the results of an operation attempt, guiding the subsequent
     * retry logic on how to proceed based on the type of failure encountered.
     */
    private enum RetryType {

        /**
         * Indicates that no retry should be attempted.
         */
        NONE,

        /**
         * Indicates that all retry attempts have been exhausted.
         */
        EXHAUSTED,

        /**
         * Indicates that the retry was triggered due to a timeout.
         */
        TIMEOUT,

        /**
         * Indicates that the operation should be retried.
         */
        RETRY
    }

}
