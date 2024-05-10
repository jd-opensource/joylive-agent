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
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;
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
@Getter
@Consumer
public class RetryPolicy extends PolicyId implements PolicyInheritWithId<RetryPolicy> {

    /**
     * Retryer implementation type, default is Resilience4j.
     */
    @Setter
    private String type = "Resilience4j";

    /**
     * The number of retry attempts that should be made in case of a failure. This parameter allows the system
     * to attempt to recover from transient failures by retrying the failed operation.
     */
    @Setter
    private Integer retry;

    /**
     * Retry waiting interval, in milliseconds.
     */
    @Setter
    private Long retryInterval;

    /**
     * Retry execution timeout, in milliseconds.
     */
    @Setter
    private Long timeout;

    /**
     * Collection of retry status codes. This parameter specifies which status codes should be considered retryable.
     */
    @Setter
    private Set<String> retryStatuses;

    /**
     * A collection of retryable exception class names.
     */
    @Setter
    private Set<String> retryExceptions;

    /**
     * The version of the policy.
     */
    private transient long version;

    @Override
    public void supplement(RetryPolicy source) {
        if (source == null) {
            return;
        }
        if (type == null) {
            type = source.type;
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
        if ((retryStatuses == null || retryStatuses.isEmpty()) && source.retryStatuses != null) {
            retryStatuses = source.retryStatuses;
        }
        if ((retryExceptions == null || retryExceptions.isEmpty()) && source.retryExceptions != null) {
            retryExceptions = source.retryExceptions;
        }
    }

    public boolean isEnabled() {
        return retry != null && retry > 0;
    }

    public boolean isRetry(String status) {
        return isEnabled() && status != null && retryStatuses != null && retryStatuses.contains(status);
    }

    public boolean isRetry(Throwable throwable) {
        if (!isEnabled() || throwable == null || retryExceptions == null || retryExceptions.isEmpty()) {
            return false;
        }
        Class<?> type = throwable.getClass();
        while (type != null && type != Object.class) {
            if (retryExceptions.contains(type.getName())) {
                return true;
            }
            type = type.getSuperclass();
        }
        return false;
    }

    protected int version() {
        return Objects.hash(id, type, retry, retryInterval, timeout, retryStatuses, retryExceptions);
    }

    protected void cache() {
        version = version();
    }
}
