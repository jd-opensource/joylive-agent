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

    private Map<String, String> lanes;

    /**
     * Types of fallback strategies for lane redirection
     * <p>
     */
    private FallbackType fallbackType;

    /**
     * When the fallbackType type is CUSTOM, the lane request is redirected based on the lane specified in this field.
     * If there is still no corresponding instance, the request is denied.
     * <p>
     */
    private String fallbackLane;

    @Override
    public void supplement(LanePolicy source) {
        if (source == null) {
            return;
        }
        if (laneSpaceId == null) {
            laneSpaceId = source.getLaneSpaceId();
        }
        if (lanes == null && source.getLanes() != null) {
            lanes = new HashMap<>(source.getLanes());
        }
        if (fallbackType == null) {
            fallbackType = source.getFallbackType();
        }
        if (fallbackLane == null) {
            fallbackLane = source.getFallbackLane();
        }
    }

    public FallbackType getFallbackType() {
        if (fallbackType == null
                || fallbackType == FallbackType.CUSTOM && (fallbackLane == null || fallbackLane.isEmpty())) {
            return FallbackType.DEFAULT;
        }
        return fallbackType;
    }

    public String getTarget(String lane) {
        return lane == null || lane.isEmpty() ? null : lanes.get(lane);
    }
}
