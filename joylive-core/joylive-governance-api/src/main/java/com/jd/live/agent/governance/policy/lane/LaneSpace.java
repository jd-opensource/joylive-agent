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
import com.jd.live.agent.core.util.cache.UnsafeLazyObject;
import com.jd.live.agent.core.util.map.ListBuilder;
import com.jd.live.agent.core.util.matcher.Matcher;
import com.jd.live.agent.governance.rule.tag.TagCondition;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

import static com.jd.live.agent.core.util.CollectionUtils.toMap;

@ToString
public class LaneSpace {

    @Getter
    @Setter
    private String id;

    @Getter
    @Setter
    private String name;

    @Getter
    @Setter
    private boolean defaultSpace;

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
    private long version;

    @Getter
    @Setter
    private Set<String> topics;

    private final transient Cache<String, Lane> laneCache = new MapCache<>(new ListBuilder<>(() -> lanes, Lane::getCode));

    private final transient Cache<String, LaneDomain> domainCache = new MapCache<>(() -> {
        if (domains != null && !domains.isEmpty()) {
            return toMap(domains, LaneDomain::getHost, e -> e);
        } else if (rules != null && !rules.isEmpty()) {
            Map<String, LaneDomain> map = new HashMap<>();
            for (LaneRule rule : rules) {
                String host = rule.getHost();
                if (host != null && !host.isEmpty()) {
                    LaneDomain domain = map.computeIfAbsent(host, k -> new LaneDomain());
                    domain.addRule(rule.getId());
                    domain.cache();
                }
            }
            return map;
        }
        return null;
    });

    private final transient Cache<String, LaneRule> ruleCache = new MapCache<>(new ListBuilder<>(() -> rules, LaneRule::getId));

    private final transient UnsafeLazyObject<Lane> defaultLaneCache = new UnsafeLazyObject<>(() -> {
        for (Lane lane : lanes) {
            if (lane.isDefaultLane()) {
                return lane;
            }
        }
        return null;
    });

    @Getter
    private transient Lane currentLane;

    public LaneRule getLaneRule(final String id) {
        return id == null ? null : ruleCache.get(id);
    }

    public Lane getLane(final String code) {
        return defaultSpace && (code == null || code.isEmpty()) ? defaultLaneCache.get() : laneCache.get(code);
    }

    public Lane getOrDefault(final String code) {
        return defaultSpace && (code == null || code.isEmpty()) ? defaultLaneCache.get() : laneCache.get(code, defaultLaneCache::get);
    }

    public Lane getDefaultLane() {
        return defaultLaneCache.get();
    }

    public LaneDomain getDomain(final String host) {
        return domainCache.get(host);
    }

    public int getDomainSize() {
        return domains == null ? 0 : domains.size();
    }

    public int getRuleSize() {
        return rules == null ? 0 : rules.size();
    }

    public boolean withTopic(final String topic) {
        return topic != null && topics != null && topics.contains(topic);
    }

    public void locate(final String lane) {
        currentLane = getLane(lane);
    }

    public String getLane(final List<String> ruleIds, final Matcher<TagCondition> matcher) {
        if (getRuleSize() == 0) {
            return null;
        }
        if (ruleIds == null || ruleIds.isEmpty()) {
            return getLane(rules, r -> r, matcher);
        }
        return getLane(ruleIds, this::getLaneRule, matcher);
    }

    public void cache() {
        getLane("");
        getDomain("");
        getLaneRule("");
        if (name == null || name.isEmpty()) {
            name = id;
        }
        if (domains != null) {
            domains.forEach(LaneDomain::cache);
        }
    }

    /**
     * Selects a lane code based on weighted random selection from matching rules.
     *
     * @param <T> the type of rule objects
     * @param rules the list of rule objects to evaluate
     * @param function the function to extract LaneRule from rule object
     * @param matcher the matcher to check if rules match conditions
     * @return the selected lane code, default lane code if no selection made, or null if no rules match
     */
    private <T> String getLane(final List<T> rules, final Function<T, LaneRule> function, final Matcher<TagCondition> matcher) {
        double seed = -1;
        double ratio = 0;
        LaneRule rule;
        for (T r : rules) {
            rule = function.apply(r);
            if (rule.match(matcher)) {
                // total ratio when multiple rules matching
                ratio += rule.getRatio();
                if (ratio >= 1) {
                    return rule.getLaneCode();
                }
                if (seed < 0) {
                    // initialize
                    seed = ThreadLocalRandom.current().nextDouble();
                }
                if (seed < ratio) {
                    return rule.getLaneCode();
                }
            }
        }
        if (ratio > 0) {
            Lane lane = getDefaultLane();
            return lane != null ? lane.getCode() : null;
        }
        return null;
    }

}
