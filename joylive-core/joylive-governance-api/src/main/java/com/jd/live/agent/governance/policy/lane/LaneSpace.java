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
package com.jd.live.agent.governance.policy.lane;

import com.jd.live.agent.core.util.cache.Cache;
import com.jd.live.agent.core.util.cache.MapCache;
import com.jd.live.agent.core.util.map.ListBuilder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Set;

public class LaneSpace {

    @Getter
    @Setter
    private String id;

    @Getter
    @Setter
    private List<Lane> lanes;

    @Getter
    @Setter
    private List<LaneDomain> domains;

    @Getter
    @Setter
    private List<LaneRule> rules;

    @Getter
    @Setter
    private Set<String> topics;

    private final transient Cache<String, Lane> laneCache = new MapCache<>(new ListBuilder<>(() -> lanes, Lane::getCode));

    private final transient Cache<String, LaneDomain> domainCache = new MapCache<>(new ListBuilder<>(() -> domains, LaneDomain::getHost));

    private final transient Cache<String, LaneRule> ruleCache = new MapCache<>(new ListBuilder<>(() -> rules, LaneRule::getId));

    @Getter
    private transient Lane defaultLane;

    @Getter
    private transient Lane currentLane;

    public LaneRule getLaneRule(String id) {
        return id == null ? null : ruleCache.get(id);
    }

    public Lane getLane(String code) {
        return laneCache.get(code);
    }

    public LaneDomain getDomain(String host) {
        return domainCache.get(host);
    }

    public boolean withTopic(String topic) {
        return topic != null && topics != null && topics.contains(topic);
    }

    public void locate(String lane) {
        currentLane = getLane(lane);
    }

    public void cache() {
        if (defaultLane == null && lanes != null) {
            for (Lane lane : lanes) {
                if (lane.isDefaultLane()) {
                    defaultLane = lane;
                }
            }
        }
        getLane("");
        getDomain("");
        getLaneRule("");
        if (domains != null) {
            domains.forEach(LaneDomain::cache);
        }
    }

}
