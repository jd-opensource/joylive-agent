package com.jd.live.agent.governance.policy.service.lane;

import com.jd.live.agent.governance.policy.PolicyId;
import com.jd.live.agent.governance.policy.PolicyInherit.PolicyInheritWithIdGen;
import com.jd.live.agent.governance.policy.service.annotation.Provider;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents a policy associated with a specific lane or pathway within a system, identified by a lane space ID.
 * This class is designed to manage policies related to individual lanes, such as traffic management, access control,
 * or resource allocation policies specific to certain pathways or channels.
 * <p>
 * The {@code LanePolicy} class extends {@link PolicyId} to uniquely identify each policy instance and implements
 * {@link PolicyInheritWithIdGen} to support policy inheritance and supplementation. This allows for flexible and
 * dynamic policy management where lane-specific policies can inherit or supplement their configurations from other
 * lane policies.
 * </p>
 *
 * @since 1.0.0
 */
@Getter
@Setter
@Provider
public class LanePolicy extends PolicyId implements PolicyInheritWithIdGen<LanePolicy> {

    /**
     * The query parameter name for the lane space ID.
     */
    public static final String QUERY_LANE_SPACE_ID = "laneSpaceId";

    /**
     * The unique identifier of the lane space to which this policy applies. This ID is used to associate the policy
     * with a specific lane or pathway within a system, enabling targeted policy enforcement and management.
     */
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
