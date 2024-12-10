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

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 * A class representing a code policy.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CodePolicy implements Cloneable {

    /**
     * Code parser
     */
    private String parser;

    /**
     * Code expression
     */
    private String expression;

    private Set<String> statuses;

    /**
     * Code parser
     */
    private Set<String> contentTypes;

    /**
     * Checks if the given status code and content type match the configured values.
     *
     * @param status the status code to check
     * @param contentType the content type to check
     * @param okStatus the OK status code to consider as a match
     * @return true if the status code and content type match, false otherwise
     */
    public boolean match(Integer status, String contentType, Integer okStatus) {
        return match(status == null ? null : status.toString(), contentType, okStatus == null ? null : okStatus.toString());
    }

    /**
     * Checks if the given status code and content type match the configured values.
     *
     * @param status      the status code to check
     * @param contentType the content type to check
     * @param okStatus    the OK status code to consider as a match
     * @return true if the status code and content type match, false otherwise
     */
    public boolean match(String status, String contentType, String okStatus) {
        return (
                status != null && status.equals(okStatus)
                        || statuses == null || statuses.isEmpty()
                        || status != null && statuses.contains(status))
                && (contentTypes == null || contentTypes.isEmpty() || contentType != null && contentTypes.contains(contentType));
    }

    /**
     * Checks if the body of the code should be parsed.
     *
     * @return true if the body of the code should be parsed, false otherwise.
     */
    public boolean isDependentOnResponseBody() {
        return parser != null && expression != null && !parser.isEmpty() && !expression.isEmpty();
    }

    @Override
    public CodePolicy clone() {
        try {
            return (CodePolicy) super.clone();
        } catch (CloneNotSupportedException e) {
            return new CodePolicy(parser, expression, statuses, contentTypes);
        }
    }

    public void cache() {
        if (contentTypes != null) {
            Set<String> lowerCases = new HashSet<>(contentTypes);
            contentTypes.forEach(o -> lowerCases.add(o.toLowerCase()));
            contentTypes = lowerCases;
        }
    }
}
