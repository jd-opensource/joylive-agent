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
package com.jd.live.agent.governance.util;

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.governance.exception.ErrorCause;
import com.jd.live.agent.governance.exception.ErrorPolicy;
import com.jd.live.agent.governance.exception.ErrorPredicate;
import com.jd.live.agent.governance.policy.service.exception.ErrorParser;
import com.jd.live.agent.governance.policy.service.exception.ErrorParserPolicy;
import com.jd.live.agent.governance.request.ServiceRequest.OutboundRequest;
import com.jd.live.agent.governance.response.ServiceResponse;

import java.util.function.Function;
import java.util.function.Predicate;

import static com.jd.live.agent.governance.exception.ErrorCause.cause;

/**
 * Utility class for Predicates.
 */
public class Predicates {

    private static final Logger logger = LoggerFactory.getLogger(Predicates.class);

    /**
     * Checks if the given ServiceResponse matches the specified ErrorPolicy.
     *
     * @param policy    the ErrorPolicy to check against
     * @param request   the OutboundRequest associated with the ServiceResponse
     * @param response  the ServiceResponse to check
     * @param predicate the ErrorPredicate to use for error cause matching
     * @param factory   the Function to use for parsing error codes
     * @return true if the ServiceResponse matches the ErrorPolicy, false otherwise
     */
    public static boolean isError(ErrorPolicy policy,
                                  OutboundRequest request,
                                  ServiceResponse response,
                                  ErrorPredicate predicate,
                                  Function<String, ErrorParser> factory) {
        // parse code
        ErrorParserPolicy errorPolicy = policy.getCodePolicy();
        boolean codeEnabled = isValid(errorPolicy) && response.match(errorPolicy);
        if (codeEnabled) {
            if (isError(errorPolicy, response, factory, policy::containsErrorCode)) {
                return true;
            }
        } else if (policy.containsErrorCode(response.getCode())) {
            return true;
        }
        // parse message
        ErrorParserPolicy messagePolicy = policy.getMessagePolicy();
        boolean messageEnabled = isValid(messagePolicy) && response.match(messagePolicy);
        if (messageEnabled) {
            if (isError(messagePolicy, response, factory, policy::containsErrorMessage)) {
                return true;
            }
        }
        ErrorCause cause = cause(response.getError(), request.getErrorFunction(), predicate);
        return cause != null && cause.match(policy, !codeEnabled, !messageEnabled);
    }

    /**
     * Checks if the given error parser policy is valid.
     *
     * @param policy The error parser policy to validate
     * @return true if the policy is valid, false otherwise
     */
    private static boolean isValid(ErrorParserPolicy policy) {
        return policy != null && policy.isValid();
    }

    /**
     * Checks if the given response matches the error condition specified by the error parser policy.
     *
     * @param policy    the error parser policy
     * @param response  the service response
     * @param factory   the error parser factory
     * @param predicate the predicate to apply to the parsed value
     * @return true if the response matches the error condition, false otherwise
     */
    private static boolean isError(ErrorParserPolicy policy,
                                   ServiceResponse response,
                                   Function<String, ErrorParser> factory,
                                   Predicate<String> predicate) {
        if (policy != null) {
            String value = parseValue(policy, response.getResult(), factory);
            return predicate.test(value);
        }
        return false;
    }

    /**
     * Parses the error code from the given result object using the specified code policy.
     *
     * @param policy  the code policy to use for parsing
     * @param result  the result object to parse
     * @param factory the code parser factory
     * @return the parsed error code, or null if no code could be parsed
     */
    private static String parseValue(ErrorParserPolicy policy,
                                     Object result,
                                     Function<String, ErrorParser> factory) {
        String value = null;
        if (result != null) {
            // parser and expression are valid
            String errorParser = policy.getParser();
            String errorExpression = policy.getExpression();
            ErrorParser parser = factory.apply(errorParser);
            if (parser != null) {
                try {
                    value = parser.getValue(errorExpression, result);
                } catch (Throwable e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
        return value;
    }

}
