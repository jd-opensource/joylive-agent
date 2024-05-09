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

import com.jd.live.agent.governance.policy.live.FaultType;

import java.util.Set;

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
     * Retrieves the sticky session ID associated with the request, if any.
     *
     * @return The sticky session ID as a String, or {@code null} if not applicable.
     */
    default String getStickyId() {
        return null;
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
     * Defines an interface for inbound service requests.
     * <p>
     * This interface represents requests that are received by a service from a client or another service.
     * </p>
     *
     * @author Zhiguo.Chen
     * @since 1.0.0
     */
    interface InboundRequest extends ServiceRequest {

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
    interface OutboundRequest extends ServiceRequest {

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
         * Provides a default exception to be thrown or handled when there is not any available endpoint.
         *
         * @return a {@link Throwable} representing the exception to be thrown or handled when
         * there is not any available endpoint. By default, a {@link RuntimeException} is returned.
         */
        default RuntimeException createNoAvailableEndpointException() {
            return null;
        }

    }
}

