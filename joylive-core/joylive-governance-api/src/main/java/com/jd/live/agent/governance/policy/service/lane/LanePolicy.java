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
package com.jd.live.agent.governance.policy.service.lane;

import com.jd.live.agent.governance.policy.PolicyId;
import com.jd.live.agent.governance.policy.PolicyInherit.PolicyInheritWithIdGen;
import com.jd.live.agent.governance.policy.lane.FallbackType;
import com.jd.live.agent.governance.policy.service.annotation.Provider;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a policy associated with a specific lane or pathway within a system,
 *
 * @since 1.0.0
 */
@Getter
@Setter
@Provider
public class LanePolicy extends PolicyId implements PolicyInheritWithIdGen<LanePolicy> {

    private String laneSpaceId;

    private Map<String, String> fallbackLanes;

    /**
     * Types of fallback strategies for lane redirection
     * <p>
     */
    private FallbackType fallbackType;

    @Override
    public void supplement(LanePolicy source) {
        if (source == null) {
            return;
        }
        if (laneSpaceId == null) {
            laneSpaceId = source.getLaneSpaceId();
        }
        if (fallbackLanes == null && source.getFallbackLanes() != null) {
            fallbackLanes = new HashMap<>(source.getFallbackLanes());
        }
        if (fallbackType == null) {
            fallbackType = source.getFallbackType();
        }
    }

    public String getFallbackLane(String lane) {
        return lane == null || lane.isEmpty() ? null : fallbackLanes.get(lane);
    }
}
