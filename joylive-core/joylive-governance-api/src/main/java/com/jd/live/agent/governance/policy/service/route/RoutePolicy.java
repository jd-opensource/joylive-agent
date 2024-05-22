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
package com.jd.live.agent.governance.policy.service.route;

import com.jd.live.agent.governance.policy.PolicyId;
import com.jd.live.agent.governance.policy.PolicyInherit.PolicyInheritWithIdGen;
import com.jd.live.agent.governance.policy.service.annotation.Consumer;
import com.jd.live.agent.governance.rule.tag.TagRule;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The RoutePolicy class defines the data structure of the routing policy.
 *
 * @since 1.0.0
 */
@Getter
@Setter
@Consumer
public class RoutePolicy extends PolicyId implements PolicyInheritWithIdGen<RoutePolicy>, Serializable {

    //Constant string defining query routing
    public static final String QUERY_ROUTE = "route";

    //The name of the routing policy
    private String name;

    // List of tag rules
    private List<TagRule> tagRules;

    // Order number
    private int order = 0;

    // Mark whether the list is sorted. The transient keyword indicates that this field will not be serialized.
    private transient boolean sorted;

    public RoutePolicy() {
    }

    public RoutePolicy(String name) {
        this.name = name;
    }

    @Override
    public void supplement(RoutePolicy source) {
        if (source == null) {
            return;
        }
        if ((tagRules == null || tagRules.isEmpty()) && source.getTagRules() != null) {
            tagRules = tagRules == null ? new ArrayList<>() : tagRules;
            tagRules.addAll(source.tagRules);
        }
    }

    /**
     * If the tag Rules list is non-empty and unsorted, it is sorted and sorted is marked as true.
     */
    public void cache() {
        if (tagRules != null && !sorted) {
            sorted = true;
            tagRules.sort(Comparator.comparingInt(TagRule::getOrder));
        }
    }

}