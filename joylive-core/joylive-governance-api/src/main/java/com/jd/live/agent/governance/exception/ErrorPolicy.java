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

import com.jd.live.agent.governance.policy.service.exception.ErrorParserPolicy;

import java.util.Set;

/**
 * An interface representing an error policy.
 */
public interface ErrorPolicy {

    /**
     * Returns the code policy associated with this component.
     *
     * @return the error code parse policy
     */
    ErrorParserPolicy getCodePolicy();

    /**
     * Returns a set of error codes that are considered critical by this component.
     *
     * @return a set of critical error codes
     */
    Set<String> getErrorCodes();

    /**
     * Returns the error message policy associated with this component.
     *
     * @return the error message parse policy
     */
    ErrorParserPolicy getMessagePolicy();

    /**
     * Returns a set of error messages that are considered critical by this component.
     *
     * @return a set of critical error messages
     */
    Set<String> getErrorMessages();

    /**
     * Returns a set of exceptions that are considered critical by this component.
     *
     * @return a set of critical exceptions
     */
    Set<String> getExceptions();

    /**
     * Checks if the feature or functionality is enabled.
     *
     * @return true if the feature or functionality is enabled, false otherwise.
     */
    boolean isEnabled();

    /**
     * Checks if the specified error code is present in the list of error codes.
     *
     * @param errorCode the error code to check.
     * @return {@code true} if the error code is present, {@code false} otherwise.
     */
    boolean containsErrorCode(String errorCode);

    /**
     * Checks if the error message is present in this instance.
     *
     * @param errorMessage the error message to search for
     * @return true if the error message is found, false otherwise
     */
    default boolean containsErrorMessage(String errorMessage) {
        if (errorMessage != null && !errorMessage.isEmpty()) {
            Set<String> messages = getErrorMessages();
            if (messages != null && !messages.isEmpty()) {
                for (String message : messages) {
                    if (errorMessage.contains(message)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Checks if the given class name is present in the list of exceptions.
     *
     * @param className The class name to check.
     * @return true if the class name is found in the list of exceptions, false otherwise.
     */
    boolean containsException(String className);

    /**
     * Checks if any of the given class names are present in the list of exceptions.
     *
     * @param classNames The set of class names to check.
     * @return true if any of the class names are found in the list of exceptions, false otherwise.
     */
    boolean containsException(Set<String> classNames);

    /**
     * Checks if the response body is required for error parsing.
     *
     * @return true if the response body is required for error parsing, false otherwise
     * @see ErrorParserPolicy#isValid()
     */
    default boolean isBodyRequired() {
        ErrorParserPolicy codePolicy = getCodePolicy();
        ErrorParserPolicy messagePolicy = getMessagePolicy();
        return codePolicy != null && codePolicy.isValid() || messagePolicy != null && messagePolicy.isValid();
    }

    /**
     * Checks if the given status code and content type match the configured values.
     *
     * @param status      the status code to check
     * @param contentType the content type to check
     * @param okStatus    the OK status code to consider as a match
     * @return true if the status code and content type match, false otherwise
     */
    default boolean match(Integer status, String contentType, Integer okStatus) {
        ErrorParserPolicy codePolicy = getCodePolicy();
        ErrorParserPolicy messagePolicy = getMessagePolicy();
        return codePolicy != null && codePolicy.match(status, contentType, okStatus)
                || messagePolicy != null && messagePolicy.match(status, contentType, okStatus);
    }

    /**
     * Checks if any of the exception sources are present in the set of target exceptions.
     *
     * @param sources the set of exception sources to check.
     * @param targets the set of target exceptions to check against.
     * @return true if any of the exception sources are found in the set of target exceptions, false otherwise.
     */
    static boolean containsException(Set<String> sources, Set<String> targets) {
        if (targets == null || targets.isEmpty() || sources == null || sources.isEmpty()) {
            return false;
        }
        Set<String> lows = sources.size() < targets.size() ? sources : targets;
        Set<String> mores = sources.size() < targets.size() ? targets : sources;
        for (String low : lows) {
            if (mores.contains(low)) {
                return true;
            }
        }
        return false;
    }
}
