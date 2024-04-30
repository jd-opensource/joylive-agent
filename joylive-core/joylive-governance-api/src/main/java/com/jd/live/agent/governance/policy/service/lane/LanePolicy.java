package com.jd.live.agent.governance.policy.service.lane;

import com.jd.live.agent.governance.policy.PolicyId;
import com.jd.live.agent.governance.policy.PolicyInherit.PolicyInheritWithIdGen;
import com.jd.live.agent.governance.policy.service.annotation.Provider;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Provider
public class LanePolicy extends PolicyId implements PolicyInheritWithIdGen<LanePolicy> {

    public static final String QUERY_LANE_SPACE_ID = "laneSpaceId";

    private Long laneSpaceId;

    @Override
    public void supplement(LanePolicy source) {
        if (source == null) {
            return;
        }
        if (laneSpaceId == null && source.getLaneSpaceId() != null) {
            laneSpaceId = source.getLaneSpaceId();
        }
    }
}
