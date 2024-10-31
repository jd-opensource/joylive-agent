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

import com.jd.live.agent.governance.policy.PolicyId;
import com.jd.live.agent.governance.rule.RelationType;
import com.jd.live.agent.governance.rule.tag.TagCondition;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Provides a base implementation for limiting policies. This abstract class defines common attributes
 * and functionalities that all specific limiting policies should inherit. It includes basic properties
 * such as policy name, strategy type, conditions under which the policy applies, and versioning information.
 * <p>
 * The class also outlines the mechanism for limiting by specifying an algorithm or a component (e.g.,
 * FixedWindow, LeakyBucket, TokenBucket, Sentinel, Resilience4j) that implements the actual limiting logic.
 * This allows for flexible and extensible design where different limiting strategies can be easily
 * integrated and managed.
 * </p>
 *
 * @since 1.0.0
 */
@Getter
@Setter
public abstract class AbstractLimitPolicy extends PolicyId implements LimitPolicy {

    /**
     * The name of the limiting policy.
     */
    private String name;

    /**
     * Defines how conditions are related (e.g., AND, OR) when evaluating whether the policy applies.
     */
    protected RelationType relationType;

    /**
     * A list of conditions (tags) that determine when the policy is applicable.
     */
    protected List<TagCondition> conditions;

    /**
     * The version of the limiting policy.
     */
    private long version;

    /**
     * Default constructor for creating an instance without initializing fields.
     */
    public AbstractLimitPolicy() {
    }

    /**
     * Constructs a new limiting policy with the specified name.
     *
     * @param name the name of the limiting policy
     */
    public AbstractLimitPolicy(String name) {
        this.name = name;
    }

    /**
     * Constructs a new limiting policy with detailed specifications.
     *
     * @param name        the name of the limiting policy
     * @param conditions  a list of conditions (tags) for the limiting policy
     * @param version     the version of the limiting policy
     */
    public AbstractLimitPolicy(String name, List<TagCondition> conditions, long version) {
        this(name, RelationType.AND, conditions, version);
    }

    /**
     * Constructs a new limiting policy with comprehensive specifications including relation type.
     *
     * @param name         the name of the limiting policy
     * @param relationType how conditions are related when evaluating the applicability of the policy
     * @param conditions   a list of conditions (tags) for the limiting policy
     * @param version      the version of the limiting policy
     */
    public AbstractLimitPolicy(String name, RelationType relationType,
                               List<TagCondition> conditions, long version) {
        this.name = name;
        this.relationType = relationType;
        this.conditions = conditions;
        this.version = version;
    }

    /**
     * Supplements the current limiting policy with another policy's details. This method is used
     * to inherit or override attributes from another policy. If the current policy lacks specific attributes,
     * they are filled in with the values from the source policy.
     *
     * @param source the source limiting policy to supplement from
     */
    public void supplement(AbstractLimitPolicy source) {
        if (source == null) {
            return;
        }
        if (name == null) {
            name = source.name;
        }
        if (relationType == null) {
            relationType = source.relationType;
        }
        if (conditions == null) {
            conditions = source.conditions;
        }
        if (version <= 0) {
            version = source.version;
            id = source.id;
            uri = source.uri;
        }
    }
}

