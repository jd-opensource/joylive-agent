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
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.governance.exception.ErrorCause;
import com.jd.live.agent.governance.exception.RetryException.RetryExhaustedException;
import com.jd.live.agent.governance.exception.RetryException.RetryTimeoutException;
import com.jd.live.agent.governance.exception.ServiceError;
import com.jd.live.agent.governance.instance.Endpoint;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.policy.service.ServicePolicy;
import com.jd.live.agent.governance.policy.service.cluster.ClusterPolicy;
import com.jd.live.agent.governance.policy.service.cluster.RetryPolicy;
import com.jd.live.agent.governance.policy.service.exception.ErrorParser;
import com.jd.live.agent.governance.request.Request;
import com.jd.live.agent.governance.request.ServiceRequest.OutboundRequest;
import com.jd.live.agent.governance.response.ServiceResponse.OutboundResponse;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.jd.live.agent.governance.exception.ErrorCause.cause;
import static com.jd.live.agent.governance.policy.service.cluster.RetryPolicy.DEFAULT_RETRY_INTERVAL;
import static com.jd.live.agent.governance.util.Predicates.isError;

/**
 * An implementation of {@link ClusterInvoker} that provides failover support for service requests.
 * This invoker is designed to automatically retry a failed request based on a defined {@link RetryPolicy}.
 * The failover mechanism is essential for enhancing the reliability and availability of service invocations
 * by rerouting failed requests to alternative instances within the cluster.
 */
@Injectable
@Extension(value = ClusterInvoker.TYPE_FAILOVER, order = ClusterInvoker.ORDER_FAILOVER)
public class FailoverClusterInvoker extends AbstractClusterInvoker {

    @Inject
    private Map<String, ErrorParser> codeParsers;

    @Override
    public <R extends OutboundRequest,
            O extends OutboundResponse,
            E extends Endpoint> CompletionStage<O> execute(final LiveCluster<R, O, E> cluster,
                                                           final OutboundInvocation<R> invocation,
                                                           final ClusterPolicy defaultPolicy) {
        ServicePolicy servicePolicy = invocation.getServiceMetadata().getServicePolicy();
        RetryPolicy defaultRetryPolicy = defaultPolicy == null ? null : defaultPolicy.getRetryPolicy();
        RetryPolicy retryPolicy = servicePolicy == null ? defaultRetryPolicy : servicePolicy.getRetryPolicy(defaultRetryPolicy);
        if (!isRetryable(retryPolicy, invocation.getRequest())) {
            // Retry is disabled, so we can just execute the request directly.
            return super.execute(cluster, invocation, defaultPolicy);
        }
        RetryContext<R, O, E> retryContext = new RetryContext<>(cluster, invocation, retryPolicy, codeParsers);
        // TODO test degrade when retry
        return retryContext.execute()
                .exceptionally(e ->
                        cluster.createResponse(cluster.createException(e, invocation), invocation.getRequest(), null));
    }

    /**
     * Checks if retry is enabled for the request based on policy.
     *
     * @param retryPolicy retry policy configuration
     * @param request     outbound request
     * @return true if retry is enabled and applicable, false otherwise
     */
    protected boolean isRetryable(final RetryPolicy retryPolicy, final OutboundRequest request) {
        return retryPolicy != null && retryPolicy.isEnabled() && retryPolicy.containsMethod(request.getMethod());
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
    protected static class RetryContext<R extends OutboundRequest,
            O extends OutboundResponse,
            E extends Endpoint> {

        /**
         * The cluster managing the distribution and processing of the request
         */
        protected final LiveCluster<R, O, E> cluster;

        protected final OutboundInvocation<R> invocation;

        protected final InvocationContext context;

        protected final R request;

        protected final Thread thread;

        /**
         * The retry policy defining the rules for retrying the operation.
         */
        protected final RetryPolicy retryPolicy;

        protected final Map<String, ErrorParser> errorParsers;

        /**
         * A counter tracking the number of retry attempts made.
         */
        protected final AtomicInteger counter = new AtomicInteger(0);

        /**
         * The deadline timestamp in milliseconds by which the operation should complete.
         */
        protected final long deadline;

        protected final long retryInterval;

        /**
         * Constructs a new {@code RetryContext} with the specified retry policy and response function.
         *
         * @param retryPolicy The {@link RetryPolicy} to govern retry behavior.
         * @param cluster     The {@link LiveCluster} managing the distribution and processing of the request
         */
        RetryContext(LiveCluster<R, O, E> cluster,
                     OutboundInvocation<R> invocation,
                     RetryPolicy retryPolicy,
                     Map<String, ErrorParser> errorParsers) {
            this.cluster = cluster;
            this.invocation = invocation;
            this.context = invocation.getContext();
            this.request = invocation.getRequest();
            this.retryPolicy = retryPolicy;
            this.errorParsers = errorParsers;
            this.deadline = retryPolicy.getDeadline(System.currentTimeMillis());
            this.thread = Thread.currentThread();
            this.retryInterval = retryPolicy.getRetryInterval(DEFAULT_RETRY_INTERVAL);
        }

        /**
         * Initiates the execution of the operation with retry logic.
         *
         * @return A {@link CompletionStage} representing the eventual completion of the operation,
         * either successfully or with an error.
         */
        public CompletionStage<O> execute() {
            onStartRequest();
            CompletableFuture<O> result = new CompletableFuture<>();
            doExecute(result);
            return result;
        }

        /**
         * Prepares request for failover retry by setting failover flag and error policy.
         */
        protected void onStartRequest() {
            request.setAttribute(Request.KEY_FAILOVER_REQUEST, Boolean.TRUE);
            request.addErrorPolicy(retryPolicy);
            cluster.onStart(request);
        }

        /**
         * Recursively executes the operation, applying retry logic and completing the future
         * based on the outcome of each attempt.
         *
         * @param future     The {@link CompletableFuture} to be completed with the operation's result.
         */
        protected void doExecute(final CompletableFuture<O> future) {
            if (!isReady()) {
                onUnready(future);
                return;
            }
            int count = onExecute();
            doInvoke().whenComplete((v, e) -> {
                ServiceError se = v == null ? null : v.getError();
                Throwable throwable = e != null || se == null ? e : se.getThrowable();
                RetryType retryType = isRetryable(request, v, e, count);
                switch (retryType) {
                    case RETRY:
                        onRetry(future, throwable);
                        break;
                    case EXHAUSTED:
                        onExhausted(future, throwable);
                        break;
                    case TIMEOUT:
                        onTimeout(future, throwable);
                        break;
                    default:
                        cluster.onRetryComplete(future, request, v, e);
                }
            });
        }

        /**
         * Checks system availability for retry operations.
         *
         * @return true if cluster is not destroyed and application is ready, false otherwise
         */
        protected boolean isReady() {
            return !cluster.isDestroyed() && context.isReady();
        }

        /**
         * Executes invocation with incremented retry counter.
         */
        protected CompletionStage<O> doInvoke() {
            return new Invoker<>(cluster, invocation, counter.getAndIncrement()).execute();
        }

        /**
         * Handles cluster unavailability by completing the future with a reject exception.
         * Called when cluster is destroyed or application is not ready for retry operations.
         *
         * @param future the completable future to complete with rejection
         */
        protected void onUnready(CompletableFuture<O> future) {
            cluster.onRetryComplete(future, request, null, cluster.createException(new RejectUnreadyException(), request));
        }

        /**
         * Handles retry attempt preparation and notification.
         *
         * @return the current retry count
         */
        protected int onExecute() {
            int count = counter.get();
            if (count > 0) {
                invocation.resetOnRetry();
            }
            cluster.onRetry(request, count);
            return count;
        }

        /**
         * Handles retry attempt scheduling.
         * Checks cluster status and deadline before scheduling next retry.
         *
         * @param future    completable future to complete
         * @param throwable current exception
         */
        protected void onRetry(CompletableFuture<O> future, Throwable throwable) {
            if (!isReady()) {
                onUnready(future);
            } else if (retryInterval + System.currentTimeMillis() > deadline) {
                onTimeout(future, throwable);
            } else if (Thread.currentThread() != thread) {
                // async
                context.getRetryExecutor().submit(() -> doExecute(future), retryInterval, TimeUnit.MILLISECONDS);
            } else {
                // sync
                try {
                    Thread.sleep(retryInterval);
                    doExecute(future);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    onTimeout(future, throwable);
                }
            }
        }

        /**
         * Handles retry timeout completion.
         * Completes future with timeout exception when retry deadline is exceeded.
         *
         * @param future    completable future to complete
         * @param throwable current exception
         */
        protected void onTimeout(CompletableFuture<O> future, Throwable throwable) {
            cluster.onRetryComplete(future, request, null, new RetryTimeoutException("retry is timeout.", throwable, retryPolicy.getTimeout()));
        }

        /**
         * Handles retry exhaustion completion.
         * Completes future when maximum retry attempts are reached.
         *
         * @param future    completable future to complete
         * @param throwable current exception
         */
        protected void onExhausted(CompletableFuture<O> future, Throwable throwable) {
            cluster.onRetryComplete(future, request, null, new RetryExhaustedException("max retries is reached out.", throwable, retryPolicy.getRetry()));
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
        protected RetryType isRetryable(R request, O response, Throwable e, int count) {
            if (invocation.isEmpty()) {
                // not retry when empty instance
                return RetryType.NONE;
            } else if (e instanceof Unretryable) {
                return RetryType.NONE;
            } else {
                ErrorCause cause = cause(e, request.getErrorFunction(), response == null ? null : response.getRetryPredicate());
                if (cause != null && cause.match(retryPolicy)
                        || response != null && isError(retryPolicy, request, response, response.getRetryPredicate(), errorParsers::get)) {
                    if (count > 0 && deadline > 0 && System.currentTimeMillis() > deadline) {
                        return RetryType.TIMEOUT;
                    } else if (count >= retryPolicy.getRetry()) {
                        return RetryType.EXHAUSTED;
                    }
                    return RetryType.RETRY;
                } else {
                    return RetryType.NONE;
                }
            }
        }
    }

    /**
     * Defines the types of retry behavior that can be encountered during operations that support retry logic.
     * This enumeration is used to categorize the results of an operation attempt, guiding the subsequent
     * retry logic on how to proceed based on the type of failure encountered.
     */
    protected enum RetryType {

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
