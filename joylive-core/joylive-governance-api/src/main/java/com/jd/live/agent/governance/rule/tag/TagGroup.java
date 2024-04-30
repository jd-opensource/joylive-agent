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
package com.jd.live.agent.governance.rule.tag;

import com.jd.live.agent.governance.rule.ConditionalMatcher;
import com.jd.live.agent.governance.rule.RelationType;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * The TagGroup class represents a group of tag conditions that can be evaluated together
 * to determine if a set of tags matches certain criteria. It implements the ConditionalMatcher
 * interface, allowing it to be used in conditional matching scenarios where tags are involved.
 *
 * @since 1.0.0
 */
@Getter
@Setter
public class TagGroup implements ConditionalMatcher<TagCondition> {

    /**
     * The list of tag conditions that this group represents.
     */
    protected List<TagCondition> conditions;

    /**
     * The logical relation type that combines the conditions within this group.
     * Defaults to {@code RelationType.AND}.
     */
    protected RelationType relationType = RelationType.AND;

    /**
     * The order of this tag group, used for sorting or prioritization purposes.
     */
    private int order = 0;

    public TagGroup() {
    }

    public TagGroup(List<TagCondition> conditions) {
        this.conditions = conditions;
    }

    public TagGroup(List<TagCondition> conditions, RelationType relationType) {
        this.relationType = relationType;
        this.conditions = conditions;
    }

    public TagGroup(List<TagCondition> conditions, RelationType relationType, int order) {
        this.conditions = conditions;
        this.relationType = relationType;
        this.order = order;
    }
}
