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
package com.jd.live.agent.governance.invoke.exception;

import com.jd.live.agent.governance.policy.service.circuitbreak.CircuitBreakPolicy;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

@Getter
@AllArgsConstructor
public class ErrorCause {

    private Throwable cause;

    private String errorCode;

    private Set<String> exceptions;

    /**
     * Builds an ErrorCause object from the given throwable object and error name function.
     *
     * @param throwable The throwable object to build the ErrorCause from.
     * @param nameFunc  The function to extract the error name and code from each cause of the throwable.
     * @return An ErrorCause object containing the root cause, error code, and set of exception names.
     */
    public static ErrorCause cause(Throwable throwable, Function<Throwable, ErrorName> nameFunc) {
        String errorCode = null;
        Set<String> exceptions = new HashSet<>(4);
        Throwable cause = null;
        Throwable candiate = null;
        Throwable t = throwable;
        ErrorName errorName;
        while (t != null) {
            errorName = nameFunc.apply(t);
            if (errorName != null) {
                if (errorName.getName() != null) {
                    exceptions.add(errorName.getName());
                    if (cause == null) {
                        cause = t;
                    }
                }
                if (errorCode == null && errorName.getCode() != null) {
                    errorCode = errorName.getCode();
                }
                if (candiate == null) {
                    candiate = t;
                }
            }
            t = t.getCause() != t ? t.getCause() : null;
        }
        cause = cause == null ? (candiate == null ? throwable : candiate) : cause;
        return new ErrorCause(cause, errorCode, exceptions);
    }

    /**
     * Checks if the given circuit breaker policy matches the error code or exceptions.
     *
     * @param policy the circuit breaker policy to match against
     * @return true if the policy matches the error code or exceptions, false otherwise
     */
    public boolean match(CircuitBreakPolicy policy) {
        return policy != null && (policy.containsError(errorCode) || policy.containsException(exceptions));
    }
}
