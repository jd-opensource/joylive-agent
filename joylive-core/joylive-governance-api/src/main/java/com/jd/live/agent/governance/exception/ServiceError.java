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

import static com.jd.live.agent.core.Constants.EXCEPTION_MESSAGE_LABEL;
import static com.jd.live.agent.core.Constants.EXCEPTION_NAMES_LABEL;
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
        // get message and exception names from header
        String message = func == null ? null : func.apply(EXCEPTION_MESSAGE_LABEL);
        String names = func == null ? null : func.apply(EXCEPTION_NAMES_LABEL);
        try {
            message = message == null || message.isEmpty()
                    ? message
                    : URLDecoder.decode(message, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException ignored) {
        }
        Set<String> exceptionNames = names == null || names.isEmpty() ? null : new LinkedHashSet<>(asList(split(names)));
        if (message != null && !message.isEmpty() || exceptionNames != null && !exceptionNames.isEmpty()) {
            return new ServiceError(message, exceptionNames, true);
        }
        return null;
    }

}
