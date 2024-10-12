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

import com.jd.live.agent.bootstrap.exception.RejectException.RejectUnreadyException;
import com.jd.live.agent.bootstrap.exception.Unretryable;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.governance.exception.ErrorCause;
import com.jd.live.agent.governance.exception.RetryException.RetryExhaustedException;
import com.jd.live.agent.governance.exception.RetryException.RetryTimeoutException;
import com.jd.live.agent.governance.exception.ServiceError;
import com.jd.live.agent.governance.instance.Endpoint;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.invoke.filter.route.CircuitBreakerFilter;
import com.jd.live.agent.governance.policy.service.ServicePolicy;
import com.jd.live.agent.governance.policy.service.cluster.ClusterPolicy;
import com.jd.live.agent.governance.policy.service.cluster.RetryPolicy;
import com.jd.live.agent.governance.policy.service.exception.CodeParser;
import com.jd.live.agent.governance.policy.service.exception.CodePolicy;
import com.jd.live.agent.governance.request.Request;
import com.jd.live.agent.governance.request.ServiceRequest.OutboundRequest;
import com.jd.live.agent.governance.response.ServiceResponse.OutboundResponse;

import java.util.HashSet;
import java.util.Map;
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

    private static final Logger logger = LoggerFactory.getLogger(CircuitBreakerFilter.class);

    @Inject
    private Map<String, CodeParser> codeParsers;

    @Override
    public <R extends OutboundRequest,
            O extends OutboundResponse,
            E extends Endpoint> CompletionStage<O> execute(LiveCluster<R, O, E> cluster,
                                                            OutboundInvocation<R> invocation,
                                                            ClusterPolicy defaultPolicy) {
        ServicePolicy servicePolicy = invocation.getServiceMetadata().getServicePolicy();
        ClusterPolicy clusterPolicy = servicePolicy == null ? null : servicePolicy.getClusterPolicy();
        RetryPolicy retryPolicy = clusterPolicy == null ? null : clusterPolicy.getRetryPolicy();
        retryPolicy = retryPolicy == null && defaultPolicy != null ? defaultPolicy.getRetryPolicy() : retryPolicy;
        if (retryPolicy != null && retryPolicy.isBodyRequest()) {
            invocation.getRequest().getAttributeIfAbsent(Request.KEY_CODE_POLICY, k -> new HashSet<CodePolicy>()).add(retryPolicy.getCodePolicy());
        }
        RetryContext<R, O, E> retryContext = new RetryContext<>(codeParsers, retryPolicy, cluster);
        Supplier<CompletionStage<O>> supplier = () -> invoke(cluster, invocation, retryContext.getCount());
        cluster.onStart(invocation.getRequest());
        return retryContext.execute(invocation.getRequest(), supplier).exceptionally(e ->
                cluster.createResponse(
                        cluster.createException(e, invocation),
                        invocation.getRequest(),
                        null));
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
     * @param <E> The type of the endpoint to which requests are routed.
     */
    private static class RetryContext<R extends OutboundRequest,
            O extends OutboundResponse,
            E extends Endpoint> {

        private final Map<String, CodeParser> errorParsers;

        /**
         * The retry policy defining the rules for retrying the operation.
         */
        private final RetryPolicy retryPolicy;

        /**
         * The cluster managing the distribution and processing of the request
         */
        private final LiveCluster<R, O, E> cluster;

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
        RetryContext(Map<String, CodeParser> errorParsers, RetryPolicy retryPolicy, LiveCluster<R, O, E> cluster) {
            this.errorParsers = errorParsers;
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
                ServiceError se = v.getError();
                Throwable throwable = se == null ? e : se.getThrowable();
                switch (isRetryable(request, v, e, count)) {
                    case RETRY:
                        Throwable unreadyException = cluster.isDestroyed() ? cluster.createException(new RejectUnreadyException(), request) : null;
                        if (unreadyException != null) {
                            future.completeExceptionally(unreadyException);
                        } else {
                            doExecute(request, supplier, future);
                        }
                        break;
                    case EXHAUSTED:
                        future.completeExceptionally(new RetryExhaustedException("max retries is reached out.", throwable, retryPolicy.getRetry()));
                        break;
                    case TIMEOUT:
                        future.completeExceptionally(new RetryTimeoutException("retry is timeout.", throwable, retryPolicy.getTimeout()));
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
            if (retryPolicy == null || !retryPolicy.isEnabled()) {
                return RetryType.NONE;
            } else if (deadline > 0 && System.currentTimeMillis() > deadline) {
                return RetryType.TIMEOUT;
            } else if (count >= retryPolicy.getRetry()) {
                return RetryType.EXHAUSTED;
            } else if (e instanceof Unretryable) {
                return RetryType.NONE;
            } else if (!retryPolicy.containsMethod(request.getMethod())) {
                return RetryType.NONE;
            } else {
                ErrorCause cause = ErrorCause.cause(e, request.getErrorFunction(), response == null ? null : response.getRetryPredicate());
                if (cause.match(retryPolicy)) {
                    return RetryType.RETRY;
                } else if (response == null) {
                    return RetryType.NONE;
                } else if (isRetryError(request, response)) {
                    return RetryType.RETRY;
                } else {
                    return RetryType.NONE;
                }
            }
        }

        /**
         * Checks if a service response represents an error, according to the circuit breaker policy.
         *
         * @param request  The request that was being processed.
         * @param response The service response to check.
         * @return true if the response represents an error, false otherwise.
         */
        private boolean isRetryError(R request, O response) {
            if (retryPolicy.containsError(response.getCode())) {
                return true;
            }
            ServiceError error = response.getError();
            Throwable throwable = error == null ? null : error.getThrowable();
            if (throwable != null) {
                return ErrorCause.cause(throwable, request.getErrorFunction(), response.getRetryPredicate()).match(retryPolicy);
            } else if (response.isSuccess() && retryPolicy.isBodyRequest()) {
                CodePolicy codePolicy = retryPolicy.getCodePolicy();
                Object result = response.getResult();
                if (result != null) {
                    String codeParser = codePolicy.getParser();
                    String codeExpression = codePolicy.getExpression();
                    if (codeParser != null && !codeParser.isEmpty() && codeExpression != null && !codeExpression.isEmpty()) {
                        CodeParser parser = errorParsers.get(codeParser);
                        if (parser != null) {
                            try {
                                String code = parser.getCode(codeExpression, result);
                                return retryPolicy.containsError(code);
                            } catch (Throwable e) {
                                logger.error("Failed to extract error code from body, caused by " + e.getMessage(), e);
                            }
                        }
                    }
                }
            }

            return false;
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
