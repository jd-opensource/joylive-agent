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

import lombok.Getter;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;

import static com.jd.live.agent.core.Constants.LABEL_EXCEPTION_MESSAGE;
import static com.jd.live.agent.core.Constants.LABEL_EXCEPTION_NAMES;
import static com.jd.live.agent.core.util.StringUtils.split;
import static java.util.Arrays.asList;

@Getter
public class ServiceError {

    private final String error;

    private final Throwable throwable;

    private final Set<String> exceptions;

    private final boolean serverError;

    public ServiceError(String error, boolean serverError) {
        this(error, null, null, serverError);
    }

    public ServiceError(Throwable throwable, boolean serverError) {
        this(null, throwable, null, serverError);
    }

    public ServiceError(String error, Set<String> exceptions, boolean serverError) {
        this(error, null, exceptions, serverError);
    }

    public ServiceError(Throwable throwable, Function<String, String> serverErrorFunc) {
        this(getErrorMessage(serverErrorFunc), throwable, getExceptionNames(serverErrorFunc), true);
    }

    public ServiceError(String error, Throwable throwable, Set<String> exceptions, boolean serverError) {
        this.error = error;
        this.throwable = throwable;
        this.exceptions = exceptions;
        this.serverError = serverError;
    }

    public boolean hasException() {
        // ignore exception names
        return throwable != null;
    }

    /**
     * Builds a ServiceError object from the given function.
     *
     * @param func a function that provides the error message and exception names
     * @return a ServiceError object if the error message or exception names are not empty, otherwise null
     */
    public static ServiceError build(Function<String, String> func) {
        String message = getErrorMessage(func);
        Set<String> exceptionNames = getExceptionNames(func);
        if (message != null && !message.isEmpty() || exceptionNames != null && !exceptionNames.isEmpty()) {
            return new ServiceError(message, exceptionNames, true);
        }
        return null;
    }

    /**
     * Returns a set of exception names based on the given function.
     *
     * @param errorFunc the function to retrieve the exception names
     * @return a set of exception names, or null if no exception names are found
     */
    private static Set<String> getExceptionNames(Function<String, String> errorFunc) {
        String names = errorFunc == null ? null : errorFunc.apply(LABEL_EXCEPTION_NAMES);
        return names == null || names.isEmpty() ? null : new LinkedHashSet<>(asList(split(names)));
    }

    /**
     * Returns the error message based on the given function.
     *
     * @param errorFunc the function to retrieve the error message
     * @return the error message, or null if no error message is found
     */
    private static String getErrorMessage(Function<String, String> errorFunc) {
        // get message and exception names from header
        String message = errorFunc == null ? null : errorFunc.apply(LABEL_EXCEPTION_MESSAGE);
        try {
            message = message == null || message.isEmpty()
                    ? message
                    : URLDecoder.decode(message, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException ignored) {
        }
        return message;
    }

}
