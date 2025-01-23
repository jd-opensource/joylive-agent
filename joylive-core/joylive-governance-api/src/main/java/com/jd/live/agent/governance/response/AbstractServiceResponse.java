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
import com.jd.live.agent.core.util.cache.CacheObject;
import com.jd.live.agent.governance.exception.ErrorPredicate;
import com.jd.live.agent.governance.exception.ServiceError;
import lombok.Getter;

import java.util.function.Supplier;

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
    protected CacheObject<ServiceError> error;

    /**
     * An optional predicate used to determine if the response should be retried.
     * A {@code null} value indicates that there is no custom retry logic, and
     * the default retryability logic will be applied.
     */
    protected final ErrorPredicate retryPredicate;

    /**
     * A supplier of service errors.
     */
    protected final Supplier<ServiceError> errorSupplier;

    /**
     * Constructs an instance of {@code AbstractServiceResponse} with the specified
     * response content, throwable, and a custom retry predicate.
     *
     * @param response       the response content
     * @param error          the error, if any, associated with the service operation
     * @param retryPredicate a custom predicate to evaluate retryability of the response
     */
    public AbstractServiceResponse(T response, ServiceError error, ErrorPredicate retryPredicate) {
        this(response, error == null ? null : () -> error, retryPredicate);
    }

    /**
     * Constructs an instance of {@code AbstractServiceResponse} with the specified
     * response content, throwable, and a custom retry predicate.
     *
     * @param response       the response content
     * @param errorSupplier  the error supplier
     * @param retryPredicate a custom predicate to evaluate retryability of the response
     */
    public AbstractServiceResponse(T response, Supplier<ServiceError> errorSupplier, ErrorPredicate retryPredicate) {
        this.response = response;
        this.errorSupplier = errorSupplier;
        this.retryPredicate = retryPredicate;
    }

    @Override
    public ServiceError getError() {
        if (error == null) {
            error = new CacheObject<>(errorSupplier != null ? errorSupplier.get() : parseError());
        }
        return error.get();
    }

    /**
     * Parses the service error from the HTTP response.
     *
     * @return The parsed service error, or null if no error was found
     */
    protected ServiceError parseError() {
        return null;
    }

}
