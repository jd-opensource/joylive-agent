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
package com.jd.live.agent.governance.invoke.matcher;

import com.jd.live.agent.governance.request.ServiceRequest;
import com.jd.live.agent.governance.rule.tag.TagCondition;

import java.util.List;

/**
 * An abstract class that implements the TagMatcher interface.
 */
public abstract class AbstractTagMatcher implements TagMatcher {

    /**
     * Matches the given tag condition against the given request.
     *
     * @param condition The tag condition to match.
     * @param request   The request to match against.
     * @return true if the condition matches the request, false otherwise.
     */
    @Override
    public boolean match(TagCondition condition, ServiceRequest request) {
        return condition.match(getValues(condition, request));
    }

    /**
     * Gets the value to be matched from the given tag condition and request.
     *
     * @param condition The tag condition.
     * @param request   The request.
     * @return The value list to be matched.
     */
    protected abstract List<String> getValues(TagCondition condition, ServiceRequest request);

}