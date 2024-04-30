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

import com.jd.live.agent.core.extension.annotation.Extensible;
import com.jd.live.agent.governance.request.Request;
import com.jd.live.agent.governance.rule.tag.TagCondition;

/**
 * The {@code TagMatcher} interface defines a contract for matching tags against specific conditions within a request.
 * Classes implementing this interface are responsible for determining if a request satisfies the criteria defined by a
 * {@code TagCondition}.
 * <p>
 * This interface is marked as extensible with the {@code @Extensible} annotation, indicating that it can be extended
 * or implemented through external plugins or services to accommodate various matching strategies.
 */
@Extensible(value = "TagMatcher")
public interface TagMatcher {

    /**
     * Determines whether a request matches the given tag condition.
     *
     * @param condition The {@code TagCondition} that encapsulates the criteria for matching.
     * @param request   The {@code Request} object containing the data to be evaluated against the condition.
     * @return {@code true} if the request meets the tag condition; {@code false} otherwise.
     */
    boolean match(TagCondition condition, Request request);
}

