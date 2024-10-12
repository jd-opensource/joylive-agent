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

import com.jd.live.agent.governance.policy.PolicyId;
import com.jd.live.agent.governance.policy.PolicyInherit.PolicyInheritWithId;
import com.jd.live.agent.governance.policy.service.annotation.Consumer;
import com.jd.live.agent.governance.policy.service.exception.CodePolicy;
import com.jd.live.agent.governance.exception.ErrorPolicy;
import lombok.Getter;
import lombok.Setter;

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
// TODO It is necessary to differentiate between read and write exceptions to simplify user configuration,
//  eliminating the need to configure the exception type on each method
@Setter
@Getter
@Consumer
public class RetryPolicy extends PolicyId implements PolicyInheritWithId<RetryPolicy>, ErrorPolicy {
    /**
     * The number of retry attempts that should be made in case of a failure. This parameter allows the system
     * to attempt to recover from transient failures by retrying the failed operation.
     */
    private Integer retry;

    /**
     * Retry waiting interval, in milliseconds.
     */
    private Long retryInterval;

    /**
     * Retry execution timeout, in milliseconds.
     */
    private Long timeout;

    /**
     * Code policy
     */
    private CodePolicy codePolicy;

    /**
     * Collection of retry status codes. This parameter specifies which status codes should be considered retryable.
     */
    private Set<String> retryStatuses;

    /**
     * A collection of retryable exception class names.
     */
    private Set<String> retryExceptions;

    @Override
    public void supplement(RetryPolicy source) {
        if (source == null) {
            return;
        }
        if (retry == null) {
            retry = source.retry;
        }
        if (retryInterval == null) {
            retryInterval = source.retryInterval;
        }
        if (timeout == null) {
            timeout = source.timeout;
        }
        if (codePolicy == null) {
            codePolicy = source.codePolicy == null ? null : source.codePolicy.clone();
        }
        if ((retryStatuses == null || retryStatuses.isEmpty()) && source.retryStatuses != null) {
            retryStatuses = source.retryStatuses;
        }
        if ((retryExceptions == null || retryExceptions.isEmpty()) && source.retryExceptions != null) {
            retryExceptions = source.retryExceptions;
        }
    }

    public long getDeadline(long startTime) {
        return timeout != null && timeout > 0 ? startTime + timeout : 0;
    }

    @Override
    public boolean isEnabled() {
        return retry != null && retry > 0 &&
                (retryStatuses != null && !retryStatuses.isEmpty()
                        || retryExceptions != null && !retryExceptions.isEmpty());
    }

    @Override
    public boolean containsError(String errorCode) {
        return errorCode != null && retryStatuses != null && retryStatuses.contains(errorCode);
    }

    @Override
    public boolean containsException(String className) {
        return className != null && retryExceptions != null && retryExceptions.contains(className);
    }

    @Override
    public boolean containsException(Set<String> classNames) {
        return ErrorPolicy.containsException(classNames, retryExceptions);
    }

    /**
     * Checks if the body of the code should be parsed.
     *
     * @return true if the body of the code should be parsed, false otherwise.
     */
    public boolean isBodyRequest() {
        return codePolicy != null && codePolicy.isBodyRequest();
    }

}
