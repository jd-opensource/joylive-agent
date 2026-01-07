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
package com.jd.live.agent.governance.policy.service.cluster;

import com.jd.live.agent.bootstrap.util.Inclusion;
import com.jd.live.agent.governance.exception.ErrorPolicy;
import com.jd.live.agent.governance.policy.PolicyId;
import com.jd.live.agent.governance.policy.PolicyInherit.PolicyInheritWithId;
import com.jd.live.agent.governance.policy.service.annotation.Consumer;
import com.jd.live.agent.governance.policy.service.exception.ErrorParserPolicy;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 * Defines a failover policy that specifies the behavior of a system or component in the event of a failure.
 * This includes how many retry attempts should be made and the timeout for each attempt before considering
 * the operation as failed. The policy is designed to improve system resilience by allowing temporary failures
 * to be retried, potentially recovering from transient issues without affecting the overall system availability.
 * <p>
 * The {@code FailoverPolicy} class implements {@link PolicyInheritWithId} interface, enabling it to inherit
 * or supplement its configuration from another failover policy. This feature is useful for dynamically adjusting
 * policy parameters in response to changes in system behavior or requirements.
 * </p>
 *
 * @since 1.0.0
 */
@Consumer
public class RetryPolicy extends PolicyId implements PolicyInheritWithId<RetryPolicy>, ErrorPolicy {

    public static final long DEFAULT_RETRY_INTERVAL = 2000L;

    /**
     * The number of retry attempts that should be made in case of a failure. This parameter allows the system
     * to attempt to recover from transient failures by retrying the failed operation.
     */
    @Setter
    @Getter
    private Integer retry;

    /**
     * Retry waiting interval, in milliseconds.
     */
    @Setter
    @Getter
    private Long interval;

    /**
     * Retry execution timeout, in milliseconds.
     */
    @Setter
    @Getter
    private Long timeout;

    /**
     * Error code policy
     */
    @Setter
    @Getter
    private ErrorParserPolicy codePolicy;

    /**
     * Collection of retry error codes. This parameter specifies which status codes should be considered retryable.
     */
    @Setter
    @Getter
    private Set<String> errorCodes;

    /**
     * Error message policy
     */
    @Setter
    @Getter
    private ErrorParserPolicy messagePolicy;

    /**
     * Collection of retry error messages. This parameter specifies which status codes should be considered retryable.
     */
    @Setter
    @Getter
    private Set<String> errorMessages;

    /**
     * A collection of retryable exception class names.
     */
    @Setter
    @Getter
    private Set<String> exceptions;

    /**
     * A set of method names that should be retried in case of failure.
     */
    @Setter
    @Getter
    private Set<String> methods;

    /**
     * A set of method name prefixes that should be retried in case of failure.
     */
    @Setter
    @Getter
    private Set<String> methodPrefixes;

    private transient Inclusion inclusion;

    @Override
    public void supplement(RetryPolicy source) {
        if (source == null) {
            return;
        }
        if (retry == null) {
            retry = source.retry;
        }
        if (interval == null) {
            interval = source.interval;
        }
        if (timeout == null) {
            timeout = source.timeout;
        }
        if (codePolicy == null) {
            codePolicy = source.codePolicy == null ? null : source.codePolicy.clone();
        }
        if ((errorCodes == null || errorCodes.isEmpty()) && source.errorCodes != null) {
            errorCodes = new HashSet<>(source.errorCodes);
        }
        if (messagePolicy == null) {
            messagePolicy = source.messagePolicy == null ? null : source.messagePolicy.clone();
        }
        if ((errorMessages == null || errorMessages.isEmpty()) && source.errorMessages != null) {
            errorMessages = new HashSet<>(source.errorMessages);
        }
        if ((exceptions == null || exceptions.isEmpty()) && source.exceptions != null) {
            exceptions = new HashSet<>(source.exceptions);
        }
        if ((methods == null || methods.isEmpty()) && source.methods != null) {
            methods = new HashSet<>(source.methods);
        }
        if ((methodPrefixes == null || methodPrefixes.isEmpty()) && source.methodPrefixes != null) {
            methodPrefixes = new HashSet<>(source.methodPrefixes);
        }
    }

    public long getDeadline(long startTime) {
        return timeout != null && timeout > 0 ? startTime + timeout : 0;
    }

    public long getRetryInterval(long defaultValue) {
        return interval != null && interval > 0 ? interval : defaultValue;
    }

    @Override
    public boolean isEnabled() {
        return retry != null && retry > 0 &&
                (errorCodes != null && !errorCodes.isEmpty()
                        || exceptions != null && !exceptions.isEmpty()
                        || errorMessages != null && !errorMessages.isEmpty());
    }

    @Override
    public boolean containsErrorCode(String errorCode) {
        return errorCode != null && errorCodes != null && errorCodes.contains(errorCode);
    }

    @Override
    public boolean containsException(String className) {
        return className != null && exceptions != null && exceptions.contains(className);
    }

    @Override
    public boolean containsException(Set<String> classNames) {
        return ErrorPolicy.containsException(classNames, exceptions);
    }

    /**
     * Checks if the specified method name should be retried in case of failure.
     *
     * @param methodName the method name to check.
     * @return true if the method name should be retried, false otherwise.
     */
    public boolean containsMethod(String methodName) {
        return inclusion.test(methodName);
    }

    public void cache() {
        inclusion = new Inclusion(methods, methodPrefixes, true);
    }

}
