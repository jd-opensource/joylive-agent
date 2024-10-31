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

import com.jd.live.agent.core.parser.json.JsonAlias;
import com.jd.live.agent.governance.policy.PolicyInherit.PolicyInheritWithIdGen;
import com.jd.live.agent.governance.policy.service.annotation.Provider;
import com.jd.live.agent.governance.rule.tag.TagCondition;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * Represents a rate limiting policy using a sliding window mechanism. This class is designed
 * to manage and enforce limits on the frequency of certain actions or operations, such as API requests,
 * to prevent abuse and ensure fair usage of resources.
 * <p>
 * The policy is configurable with one or more sliding windows, each defining a time frame and a maximum
 * number of allowed actions within that frame. This allows for flexible and fine-grained control over
 * rate limiting, accommodating various use cases and requirements.
 * </p>
 * <p>
 * This class extends {@link AbstractLimitPolicy} to leverage common limit policy features and implements
 * the {@link PolicyInheritWithIdGen} interface to support inheritance of policies with unique ID generation.
 * It is annotated with Lombok annotations {@code @Getter} and {@code @Setter} for automatic generation of
 * getter and setter methods, and {@code @Provider} to denote its role as a provider in dependency injection
 * frameworks.
 * </p>
 *
 * @since 1.0.0
 */
@Getter
@Setter
@Provider
public class RateLimitPolicy extends AbstractLimitPolicy implements PolicyInheritWithIdGen<RateLimitPolicy> {

    /**
     * A list of sliding windows that define the rate limits. Each sliding window specifies
     * a time frame and the maximum number of actions allowed within that frame.
     */
    private List<SlidingWindow> slidingWindows;

    /**
     * The maximum time, in milliseconds, a request can wait to be executed before it is rejected, when the maximum
     * concurrency or rate limit has been reached.
     */
    private Long maxWaitMs;

    /**
     * A map of parameters that further customize the action of the limiting strategy.
     */
    @JsonAlias("actionParameters")
    private Map<String, String> parameters;

    /**
     * Specifies the algorithm or component used for implementing the limiting logic.
     */
    private String realizeType;

    /**
     * Default constructor for creating an instance without initializing fields.
     */
    public RateLimitPolicy() {
        super();
    }

    /**
     * Constructs a new rate limit policy with the specified name.
     *
     * @param name the name of the rate limit policy
     */
    public RateLimitPolicy(String name) {
        super(name);
    }

    /**
     * Constructs a new rate limit policy with detailed specifications.
     *
     * @param name           the name of the rate limit policy
     * @param realizeType    the realize type of the rate limit policy
     * @param conditions     a list of conditions (tags) for the rate limit policy
     * @param slidingWindows a list of sliding windows that define the rate limits
     * @param version        the version of the rate limit policy
     */
    public RateLimitPolicy(String name, String realizeType, List<TagCondition> conditions,
                           List<SlidingWindow> slidingWindows, long version) {
        super(name, conditions, version);
        this.realizeType = realizeType;
        this.slidingWindows = slidingWindows;
    }

    /**
     * Supplements the current rate limit policy with another policy's details. If the current
     * policy does not have sliding windows defined, it inherits them from the specified source policy.
     * This method facilitates the combination or inheritance of policy attributes.
     *
     * @param source the source rate limit policy to supplement from
     */
    @Override
    public void supplement(RateLimitPolicy source) {
        if (source == null) {
            return;
        }
        super.supplement(source);
        if (maxWaitMs == null) {
            maxWaitMs = source.maxWaitMs;
        }
        if (parameters == null) {
            parameters = source.parameters;
        }
        if (slidingWindows == null) {
            slidingWindows = source.slidingWindows;
        }
        if (realizeType == null) {
            realizeType = source.realizeType;
        }
    }
}

