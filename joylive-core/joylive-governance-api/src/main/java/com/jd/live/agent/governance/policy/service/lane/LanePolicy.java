package com.jd.live.agent.governance.policy.service.lane;

import com.jd.live.agent.governance.policy.PolicyId;
import com.jd.live.agent.governance.policy.PolicyInherit.PolicyInheritWithIdGen;
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
    }

    public String getTarget(String lane) {
        return lane == null || lane.isEmpty() ? null : lanes.get(lane);
    }
}
