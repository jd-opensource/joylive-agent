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

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.jd.live.agent.core.util.ExceptionUtils.isNoneWrapped;
import static com.jd.live.agent.governance.exception.ErrorPolicy.containsException;

@AllArgsConstructor
public class ErrorCause {

    @Getter
    private Throwable cause;

    private String errorCode;

    private String errorMessage;

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
        if (matched) {
            return true;
        }
        if (policy != null && (policy.containsErrorCode(errorCode)
                || policy.containsException(exceptions)
                || policy.containsErrorMessage(errorMessage))) {
            return true;
        }
        return containsException(exceptions, targets);
    }

    /**
     * Creates an ErrorCause instance based on the provided ServiceError, name function, and error predicate.
     *
     * @param error     the ServiceError instance containing the error information
     * @param nameFunc  a function that maps a Throwable to an ErrorName
     * @param predicate an optional ErrorPredicate used to filter the error cause
     * @return an ErrorCause instance, or null if no error cause can be determined
     */
    public static ErrorCause cause(ServiceError error,
                                   Function<Throwable, ErrorName> nameFunc,
                                   ErrorPredicate predicate) {
        if (error == null) {
            return null;
        }
        Throwable throwable = error.getThrowable();
        if (throwable != null) {
            return cause(throwable, nameFunc, predicate);
        } else {
            Set<String> exceptions = error.getExceptions();
            Set<String> targets = predicate == null ? null : predicate.getExceptions();
            String errorMessage = error.getError();
            if (exceptions != null && !exceptions.isEmpty()) {
                return new ErrorCause(null, null, errorMessage, exceptions, targets, false);
            } else if (errorMessage != null && !errorMessage.isEmpty()) {
                return new ErrorCause(null, null, errorMessage, null, null, false);
            }
        }
        return null;
    }

    /**
     * Builds an ErrorCause object from the given throwable object and error name function.
     *
     * @param throwable The throwable object to build the ErrorCause from.
     * @param nameFunc  The function to extract the error name and code from each cause of the throwable.
     * @param predicate The error predicate.
     * @return An ErrorCause object containing the root cause, error code, and set of exception names.
     */
    public static ErrorCause cause(Throwable throwable,
                                   Function<Throwable, ErrorName> nameFunc,
                                   ErrorPredicate predicate) {
        if (throwable == null) {
            return null;
        }
        boolean matched = false;
        String errorCode = null;
        Set<String> targets = predicate == null ? null : predicate.getExceptions();
        Set<String> exceptions = new LinkedHashSet<>(8);
        Throwable cause = null;
        Throwable candiate = null;
        Throwable t = isNoneWrapped(throwable) ? throwable : throwable.getCause();
        ErrorName errorName;
        Predicate<Throwable> test = predicate == null ? null : predicate.getPredicate();
        while (t != null) {
            if (test != null && test.test(t)) {
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
        return new ErrorCause(cause, errorCode, throwable.getMessage(), exceptions, targets, matched);
    }
}
