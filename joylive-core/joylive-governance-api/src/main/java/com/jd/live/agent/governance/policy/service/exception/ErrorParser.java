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

import com.jd.live.agent.core.extension.annotation.Extensible;

/**
 * An interface for parsing code from service responses.
 */
@Extensible("ErrorParser")
public interface ErrorParser {

    /**
     * Parses the error from a service response using the provided expression.
     *
     * @param expression The expression used to extract the error information from the response.
     * @param response   The service response to parse.
     * @return The extracted error information as a string.
     */
    String getValue(String expression, Object response);

}
