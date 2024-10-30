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
package com.jd.live.agent.governance.request;

import com.jd.live.agent.bootstrap.exception.LiveException;
import com.jd.live.agent.governance.exception.ErrorName;
import com.jd.live.agent.governance.exception.ErrorPolicy;
import com.jd.live.agent.governance.policy.live.FaultType;
import com.jd.live.agent.governance.policy.service.circuitbreak.DegradeConfig;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

/**
 * Defines an interface for service requests, extending the basic {@link Request} interface.
 * <p>
 * This interface introduces additional methods that are common for service requests, such as accessing service metadata,
 * managing attempts, and handling failures and rejections.
 * </p>
 *
 * @since 1.0.0
 */
public interface ServiceRequest extends Request {

    /**
     * Retrieves the name of the service.
     *
     * @return The service name as a String.
     */
    String getService();

    /**
     * Retrieves the group name of the service.
     *
     * @return The group name as a String.
     */
    String getGroup();

    /**
     * Checks if the group is native.
     * <p>
     *
     * @return true if the group is native, false otherwise.
     */
    default boolean isNativeGroup() {
        return false;
    }

    /**
     * Retrieves the name of the method being called on the service.
     *
     * @return The method name as a String.
     */
    String getMethod();

    /**
     * Retrieves the path associated with the service request.
     *
     * @return The path as a String.
     */
    String getPath();

    /**
     * Returns the values of a specific header.
     *
     * @param key The name of the header.
     * @return A list of values for the specified header, or null if the header does not exist.
     */
    List<String> getHeaders(String key);

    /**
     * Returns the values of a specific header.
     *
     * @param key The name of the header.
     * @return A list of values for the specified header, or null if the header does not exist.
     */
    String getHeader(String key);

    /**
     * Returns the value of a specific query parameter.
     *
     * @param key The name of the query parameter.
     * @return The value of the specified query parameter, or null if it does not exist.
     */
    String getQuery(String key);

    /**
     * Returns the values of a specific query.
     *
     * @param key The name of the query.
     * @return A list of values for the specified query, or null if the query does not exist.
     */
    List<String> getQueries(String key);

    /**
     * Returns the values of a specific cookie.
     *
     * @param key The name of the header.
     * @return A list of values for the specified cookie, or null if the cookie does not exist.
     */
    List<String> getCookies(String key);

    /**
     * Returns the value of a specific cookie.
     *
     * @param key The name of the cookie.
     * @return The value of the specified cookie, or null if it does not exist.
     */
    String getCookie(String key);

    /**
     * Returns the start time.
     *
     * @return the start time in milliseconds since the epoch (January 1, 1970, 00:00:00 GMT).
     */
    long getStartTime();

    /**
     * Calculates the duration from the start time to the current time.
     * This is a default method that uses the {@link #getStartTime()} method to determine the start time.
     *
     * @return the duration in milliseconds from the start time to the current time.
     */
    default long getDuration() {
        return System.currentTimeMillis() - this.getStartTime();
    }

    /**
     * Indicates whether the invocation is performed asynchronously.
     * <p>
     * This default implementation returns {@code false}, indicating that the invocation is
     * performed synchronously. Subclasses should override this method if they provide asynchronous
     * invocation capabilities.
     * </p>
     *
     * @return {@code false} by default, meaning the invocation is synchronous.
     */
    default boolean isAsync() {
        return false;
    }

    /**
     * Determines if the request is a system message.
     *
     * @return {@code true} if the request is a system message; {@code false} otherwise.
     */
    default boolean isSystem() {
        return false;
    }

    /**
     * Checks if the response body is required for the given error policy.
     *
     * @param policy the error policy to check
     * @return true if the response body is required, false otherwise (default implementation always returns false)
     */
    default boolean requireResponseBody(ErrorPolicy policy) {
        return false;
    }

    /**
     * Rejects the request with the given fault type and reason.
     *
     * @param type   The type of fault.
     * @param reason The reason for the rejection.
     * @throws RuntimeException Throws a runtime exception as defined by the fault type's rejection method.
     */
    default void reject(FaultType type, String reason) {
        throw type.reject(reason);
    }

    /**
     * Initiates a failover for the request with the given fault type and reason.
     *
     * @param type   The type of fault.
     * @param reason The reason for the failover.
     * @throws RuntimeException Throws a runtime exception as defined by the fault type's failover method.
     */
    default void failover(FaultType type, String reason) {
        throw type.failover(reason);
    }

    /**
     * Initiates a degradation for the request with the given fault type and reason.
     *
     * @param type   The type of fault.
     * @param reason The reason for the failover.
     * @param config The degrade config.
     * @throws RuntimeException Throws a runtime exception as defined by the fault type's failover method.
     */
    default void degrade(FaultType type, String reason, DegradeConfig config) {
        throw type.degrade(reason, config);
    }

    /**
     * Defines an interface for inbound service requests.
     * <p>
     * This interface represents requests that are received by a service from a client or another service.
     * </p>
     *
     * @author Zhiguo.Chen
     * @since 1.0.0
     */
    interface InboundRequest extends ServiceRequest {

        /**
         * Gets the client IP address associated with the request.
         *
         * @return the client IP address.
         */
        default String getClientIp() {
            return "";
        }

    }

    /**
     * Defines an interface for outbound service requests.
     * <p>
     * This interface represents requests that are sent from a service to another service or component.
     * </p>
     *
     * @author Zhiguo.Chen
     * @since 1.0.0
     */
    interface OutboundRequest extends ServiceRequest, StickyRequest {

        Function<Throwable, ErrorName> DEFAULT_ERROR_FUNCTION =
                throwable -> throwable instanceof LiveException || throwable instanceof ExecutionException
                        ? null
                        : new ErrorName(throwable.getClass().getName(), null);

        /**
         * Retrieves a set of identifiers that represent the attempts made for this request.
         *
         * @return A Set of String identifiers for the attempts.
         */
        Set<String> getAttempts();

        /**
         * Adds an attempt identifier to this service request.
         * <p>
         * This method is used to track retries or other types of attempts related to processing the service request.
         * If the {@code attempt} parameter is not {@code null}, it will be added to the set of attempt identifiers.
         * If no attempts have previously been added, a new set will be initialized.
         * </p>
         *
         * @param attempt The identifier of the attempt to add.
         */
        void addAttempt(String attempt);

        /**
         * Retrieves the configured timeout value.
         * <p>
         * This method returns the timeout setting for the current request. The timeout
         * is expressed in milliseconds and represents the maximum time allowed for a certain
         * operation to complete. A return value of 0 may indicate that no timeout is set,
         * implying an operation could potentially wait indefinitely.
         * </p>
         *
         * @return The timeout value in milliseconds. A value of 0 may indicate no timeout.
         */
        default long getTimeout() {
            return 0;
        }

        /**
         * Sets the timeout value for the current request.
         * <p>
         * This method allows specifying a timeout in milliseconds, which determines the maximum
         * duration allowed for a certain operation to complete. Setting this value influences
         * how long a process will wait before timing out. A value of 0 can be used to indicate
         * that there should be no timeout, allowing the operation to continue indefinitely until
         * completion.
         * </p>
         *
         * @param timeout The desired timeout in milliseconds. A value of 0 indicates no timeout.
         */
        default void setTimeout(long timeout) {
        }

        /**
         * Determines if the current request is sensitive to specific instances.
         *
         * @return {@code true} if the operation is instance-sensitive, meaning it takes into
         * account the state or characteristics of individual instances; {@code false}
         * otherwise.
         */
        default boolean isInstanceSensitive() {
            return true;
        }

        /**
         * Returns the default error name function.
         *
         * @return The default error name function.
         */
        default Function<Throwable, ErrorName> getErrorFunction() {
            return DEFAULT_ERROR_FUNCTION;
        }

    }
}

