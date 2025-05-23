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
package com.jd.live.agent.governance.policy.service.exception;

import java.io.InputStream;

/**
 * Abstract base class for parsing error responses using various input formats.
 * Provides common parsing logic and delegates format-specific parsing to subclasses.
 */
public abstract class AbstractErrorParser implements ErrorParser {

    @Override
    public String getValue(String expression, Object response) {
        if (expression == null || expression.isEmpty() || response == null) {
            return null;
        }
        Object result;
        if (response instanceof String) {
            result = parse(expression, (String) response);
        } else if (response instanceof byte[]) {
            result = parse(expression, (byte[]) response);
        } else if (response instanceof InputStream) {
            result = parse(expression, (InputStream) response);
        } else {
            result = parse(expression, response);
        }
        return result == null ? null : result.toString();
    }

    /**
     * Parses response from String content.
     *
     * @param expression the parsing expression
     * @param response   the response as String
     * @return parsed value or null
     */
    protected abstract String parse(String expression, String response);

    /**
     * Parses response from InputStream.
     *
     * @param expression the parsing expression
     * @param response   the response as InputStream
     * @return parsed value or null
     */
    protected abstract String parse(String expression, InputStream response);

    /**
     * Default parsing for Object responses (converts to String).
     *
     * @param expression the parsing expression
     * @param response   the response object
     * @return parsed value or null
     */
    protected String parse(String expression, Object response) {
        return parse(expression, response.toString());
    }

    /**
     * Parses response from byte array (converts to String).
     *
     * @param expression the parsing expression
     * @param response   the response as byte array
     * @return parsed value or null
     */
    protected String parse(String expression, byte[] response) {
        return parse(expression, new String(response));
    }
}
