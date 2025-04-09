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
package com.jd.live.agent.governance.policy.service.auth;

import com.jd.live.agent.governance.policy.PolicyId;
import com.jd.live.agent.governance.policy.PolicyInherit;
import com.jd.live.agent.governance.rule.ConditionalMatcher;
import com.jd.live.agent.governance.rule.RelationType;
import com.jd.live.agent.governance.rule.tag.TagCondition;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * Permission policy
 *
 * @since 1.2.0
 */
@Getter
@Setter
public class PermissionPolicy extends PolicyId implements ConditionalMatcher<TagCondition>,
        PolicyInherit.PolicyInheritWithIdGen<PermissionPolicy>, Serializable {

    /**
     * The name of the permission policy.
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
     * Type of allow result.
     */
    private AllowResult type;

    /**
     * The version of the permission policy.
     */
    private long version;

    public PermissionPolicy() {
    }

    public PermissionPolicy(String name) {
        this.name = name;
    }

    public PermissionPolicy(String name, RelationType relationType, List<TagCondition> conditions, AllowResult type, long version) {
        this.name = name;
        this.relationType = relationType;
        this.conditions = conditions;
        this.type = type;
        this.version = version;
    }

    /**
     * Supplements the policy instance with properties or settings from the specified source object.
     * This method allows the policy to inherit or update its behavior based on the state of the source.
     *
     * @param source the source object from which to supplement properties or settings
     */
    @Override
    public void supplement(PermissionPolicy source) {
        if (source == null) {
            return;
        }
        if (name == null) {
            name = source.name;
        }
        if (relationType == null && source.getRelationType() != null) {
            relationType = source.getRelationType();
        }
        if (conditions == null && source.getConditions() != null) {
            conditions = source.getConditions();
        }
        if (type == null && source.getType() != null) {
            type = source.getType();
        }
        if (version <= 0) {
            version = source.getVersion();
        }
    }
}
