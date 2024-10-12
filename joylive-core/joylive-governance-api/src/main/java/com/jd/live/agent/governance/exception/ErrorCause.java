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
package com.jd.live.agent.governance.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

@AllArgsConstructor
public class ErrorCause {

    @Getter
    private Throwable cause;

    private String errorCode;

    private Set<String> exceptions;

    private Set<String> targets;

    private boolean matched;

    /**
     * Checks if the given circuit breaker policy matches the error code or exceptions.
     *
     * @param policy the circuit breaker policy to match against
     * @return true if the policy matches the error code or exceptions, false otherwise
     */
    public boolean match(ErrorPolicy policy) {
        return matched
                || policy != null && (policy.containsError(errorCode) || policy.containsException(exceptions))
                || ErrorPolicy.containsException(exceptions, targets);
    }

    /**
     * Builds an ErrorCause object from the given throwable object and error name function.
     *
     * @param throwable The throwable object to build the ErrorCause from.
     * @param nameFunc  The function to extract the error name and code from each cause of the throwable.
     * @return An ErrorCause object containing the root cause, error code, and set of exception names.
     */
    public static ErrorCause cause(Throwable throwable,
                                   Function<Throwable, ErrorName> nameFunc,
                                   ErrorPredicate retryPredicate) {
        boolean matched = false;
        String errorCode = null;
        Set<String> targets = retryPredicate == null ? null : retryPredicate.getExceptions();
        Set<String> exceptions = new HashSet<>(8);
        Throwable cause = null;
        Throwable candiate = null;
        Throwable t = throwable;
        ErrorName errorName;
        Predicate<Throwable> predicate = retryPredicate == null ? null : retryPredicate.getPredicate();
        while (t != null) {
            if (predicate != null && predicate.test(t)) {
                matched = true;
                break;
            }
            errorName = nameFunc.apply(t);
            if (errorName != null) {
                if (errorName.getName() != null) {
                    if (cause == null) {
                        cause = t;
                    }
                    // add exception name
                    exceptions.add(errorName.getName());
                    // add super class
                    Class<?> p = t.getClass();
                    // generic exception such as org.apache.dubbo.rpc.service.GenericException
                    p = p.getName().equals(errorName.getName()) ? p.getSuperclass() : null;
                    while (p != null && p != Object.class) {
                        if (exceptions.add(p.getName())) {
                            p = p.getSuperclass();
                        } else {
                            break;
                        }
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
        return new ErrorCause(cause, errorCode, exceptions, targets, matched);
    }
}
