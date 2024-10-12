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

import com.jd.live.agent.bootstrap.exception.RejectException.RejectCircuitBreakException;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.governance.instance.Endpoint;
import com.jd.live.agent.governance.policy.service.circuitbreak.DegradeConfig;
import com.jd.live.agent.governance.request.ServiceRequest.OutboundRequest;
import com.jd.live.agent.governance.exception.ErrorPredicate;
import com.jd.live.agent.governance.exception.ServiceError;
import com.jd.live.agent.governance.response.ServiceResponse.OutboundResponse;

import static com.jd.live.agent.bootstrap.exception.RejectException.RejectCircuitBreakException.getCircuitBreakException;

/**
 * An abstract implementation of the LiveCluster interface.
 *
 * @param <R> The type of outbound request.
 * @param <O> The type of outbound response.
 * @param <E> The type of endpoint.
 */
public abstract class AbstractLiveCluster<R extends OutboundRequest,
        O extends OutboundResponse,
        E extends Endpoint> implements LiveCluster<R, O, E> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractLiveCluster.class);

    @Override
    public O createResponse(Throwable throwable, R request, E endpoint) {
        if (throwable == null) {
            return createResponse(request);
        }
        RejectCircuitBreakException circuitBreakException = getCircuitBreakException(throwable);
        if (circuitBreakException != null) {
            DegradeConfig config = circuitBreakException.getConfig();
            if (config != null) {
                try {
                    return createResponse(request, config);
                } catch (Throwable e) {
                    logger.warn("Exception occurred when create degrade response from circuit break. caused by " + e.getMessage(), e);
                    return createResponse(new ServiceError(createException(throwable, request, endpoint), false), null);
                }
            }
        }
        return createResponse(new ServiceError(createException(throwable, request, endpoint), false), getRetryPredicate());
    }

    /**
     * Creates a response based on the provided request.
     * This method is abstract and must be implemented by subclasses.
     *
     * @param request the original request containing headers.
     * @return a response configured according to the request.
     */
    protected abstract O createResponse(R request);

    /**
     * Creates a response based on the provided request and degrade configuration.
     * This method is abstract and must be implemented by subclasses.
     *
     * @param request       the original request containing headers.
     * @param degradeConfig the degrade configuration specifying the response details such as status code, headers, and body.
     * @return a response configured according to the degrade configuration.
     */
    protected abstract O createResponse(R request, DegradeConfig degradeConfig);

    /**
     * Creates a response based on the provided service error and predicate.
     * This method is abstract and must be implemented by subclasses.
     *
     * @param error the service error to use for creating the response.
     * @param predicate a predicate to determine if the error should be used for creating the response.
     * @return a response configured according to the service error and predicate.
     */
    protected abstract O createResponse(ServiceError error, ErrorPredicate predicate);

}
