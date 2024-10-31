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
package com.jd.live.agent.governance.policy.service.limit;

import com.jd.live.agent.governance.rule.ConditionalMatcher;
import com.jd.live.agent.governance.rule.tag.TagCondition;

import java.io.Serializable;

/**
 * Represents a policy that limits actions based on specific conditions.
 * This interface extends {@link ConditionalMatcher} for matching conditions
 * and implements {@link Serializable} to allow serialization of its instances.
 *
 * <p>Limit policies are crucial in scenarios where actions need to be constrained
 * under certain conditions, providing a flexible way to define and apply such constraints.</p>
 */
public interface LimitPolicy extends ConditionalMatcher<TagCondition>, Serializable {

    /**
     * Gets the name of the limit policy.
     *
     * <p>This default method returns {@code null} indicating that the implementation
     * may optionally provide a meaningful name for the limit policy. Implementations
     * are encouraged to override this method to return a non-null, meaningful name.</p>
     *
     * @return the name of the limit policy, or {@code null} if not specified.
     */
    default String getName() {
        return null;
    }

}

