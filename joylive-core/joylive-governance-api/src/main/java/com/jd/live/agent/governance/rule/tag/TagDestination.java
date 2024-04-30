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

import com.jd.live.agent.governance.rule.RelationType;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * The TagDestination class represents a destination for tag-based routing, which includes a
 * group of tag conditions and an associated weight. It extends the TagGroup class, adding
 * weight as an additional property to influence the routing decision.
 *
 * @since 1.0.0
 */
@Getter
@Setter
public class TagDestination extends TagGroup {

    /**
     * The weight associated with this tag destination, which can be used to prioritize or
     * influence the routing decision among multiple possible destinations.
     */
    private int weight;

    /**
     * Constructs a new TagDestination with no conditions and a default weight of 0.
     */
    public TagDestination() {
    }

    /**
     * Constructs a new TagDestination with the specified list of conditions and weight.
     *
     * @param conditions the list of tag conditions for this destination
     * @param weight     the weight associated with this destination
     */
    public TagDestination(List<TagCondition> conditions, int weight) {
        super(conditions);
        this.weight = weight;
    }

    /**
     * Constructs a new TagDestination with the specified conditions, relation type, and weight.
     *
     * @param conditions the list of tag conditions for this destination
     * @param relationType the logical relation type that combines the conditions within the group
     * @param weight the weight associated with this destination
     */
    public TagDestination(List<TagCondition> conditions, int weight, RelationType relationType) {
        super(conditions, relationType);
        this.weight = weight;
    }

}

