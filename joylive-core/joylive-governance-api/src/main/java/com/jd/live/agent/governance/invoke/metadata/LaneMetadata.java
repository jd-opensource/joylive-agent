package com.jd.live.agent.governance.invoke.metadata;

import com.jd.live.agent.governance.config.LaneConfig;
import com.jd.live.agent.governance.policy.lane.Lane;
import com.jd.live.agent.governance.policy.lane.LaneSpace;
import lombok.Builder;
import lombok.Getter;

/**
 * The {@code LaneMetadata} class encapsulates the metadata for a lane request.
 */
@Getter
@Builder
public class LaneMetadata {

    /**
     * The lane configuration associated with this invocation.
     */
    private LaneConfig laneConfig;

    /**
     * The lane space context for this invocation.
     */
    private LaneSpace laneSpace;

    /**
     * The current lane context for this invocation.
     */
    private Lane currentLane;

    /**
     * The target lane for this invocation.
     */
    private Lane targetLane;
}
