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
package com.jd.live.agent.governance.response;

import com.jd.live.agent.bootstrap.util.AbstractAttributes;
import lombok.Getter;

import java.util.function.Predicate;

/**
 * Provides a base implementation for service response objects, encapsulating common
 * elements such as the response content, any exceptions that may have occurred during
 * service execution, and a predicate for retry logic. This class is designed to be
 * extended by specific service response types, allowing for a consistent approach to
 * handling responses across different services.
 *
 * @param <T> the type of the response content
 * @since 1.0.0
 */
@Getter
public abstract class AbstractServiceResponse<T> extends AbstractAttributes implements ServiceResponse {

    /**
     * The main content of the service response.
     */
    protected T response;

    /**
     * An optional Throwable capturing any error that may have occurred during the
     * service operation. A {@code null} value indicates that the operation completed
     * without errors.
     */
    protected final ServiceError error;

    /**
     * An optional predicate used to determine if the response should be retried.
     * A {@code null} value indicates that there is no custom retry logic, and
     * the default retryability logic will be applied.
     */
    protected final Predicate<Throwable> predicate;

    /**
     * Constructs an instance of {@code AbstractServiceResponse} with the specified
     * response content, throwable, and a custom retry predicate.
     *
     * @param response  the response content
     * @param error     the error, if any, associated with the service operation
     * @param predicate a custom predicate to evaluate retryability of the response
     */
    public AbstractServiceResponse(T response, ServiceError error, Predicate<Throwable> predicate) {
        this.response = response;
        this.error = error;
        this.predicate = predicate;
    }

    @Override
    public ServiceError getError() {
        return error;
    }

    @Override
    public boolean isRetryable(Throwable throwable) {
        return predicate != null && predicate.test(throwable);
    }
}
