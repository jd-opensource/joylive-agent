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

import java.io.Serializable;
import java.util.List;

/**
 * The TagRule class represents a rule for tag-based routing. It extends the TagGroup class to include
 * a set of tag conditions and adds a list of TagDestination objects. Each TagRule can be matched against
 * a set of tags to determine if the associated destinations should be considered for routing.
 *
 * @since 1.0.0
 */
@Getter
@Setter
public class TagRule extends TagGroup implements Serializable {

    /**
     * A list of TagDestination objects that this rule is associated with. These destinations
     * are the possible targets if the tag conditions of this rule are met.
     */
    private List<TagDestination> destinations;

    /**
     * Constructs a new TagRule with no conditions and no destinations.
     */
    public TagRule() {
    }

    /**
     * Constructs a new TagRule with the specified list of destinations.
     *
     * @param destinations the list of tag destinations for this rule
     */
    public TagRule(List<TagDestination> destinations) {
        this.destinations = destinations;
    }

    /**
     * Constructs a new TagRule with the specified list of conditions and destinations.
     *
     * @param conditions   the list of tag conditions for this rule
     * @param destinations the list of tag destinations for this rule
     */
    public TagRule(List<TagCondition> conditions, List<TagDestination> destinations) {
        super(conditions);
        this.destinations = destinations;
    }

    /**
     * Constructs a new TagRule with the specified conditions, relation type, and destinations.
     *
     * @param conditions the list of tag conditions for this rule
     * @param relationType the logical relation type that combines the conditions within the group
     * @param destinations the list of tag destinations for this rule
     */
    public TagRule(List<TagCondition> conditions, List<TagDestination> destinations, RelationType relationType) {
        super(conditions, relationType);
        this.destinations = destinations;
    }

    /**
     * Constructs a new TagRule with the specified conditions, relation type, order, and destinations.
     *
     * @param conditions the list of tag conditions for this rule
     * @param relationType the logical relation type that combines the conditions within the group
     * @param order the order of this tag rule
     * @param destinations the list of tag destinations for this rule
     */
    public TagRule(List<TagCondition> conditions, List<TagDestination> destinations, RelationType relationType, int order) {
        super(conditions, relationType, order);
        this.destinations = destinations;
    }
}
