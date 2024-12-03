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
     * The target lance space id for this invocation
     */
    private String targetSpaceId;

    /**
     * The target lane space context for this invocation.
     */
    private LaneSpace targetSpace;

    /**
     * The target lane id for this invocation
     */
    private String targetLaneId;

    /**
     * The target lane for this invocation.
     */
    private Lane targetLane;

    /**
     * The local lane space id for this invocation.
     */
    private String localSpaceId;

    /**
     * The local lance space for this invocation.
     */
    private LaneSpace localSpace;

    /**
     * The local lane id for this invocation.
     */
    private String localLaneId;

    /**
     * The local lane for this invocation.
     */
    private Lane localLane;

    /**
     * The default lane space id for this invocation.
     */
    private String defaultSpaceId;

    /**
     * The default lance space for this invocation.
     */
    private LaneSpace defaultSpace;

    /**
     * The default lane id for this invocation.
     */
    private String defaultLaneId;

    /**
     * The default lane for this invocation.
     */
    private Lane defaultLane;

    public Lane getTargetLaneOrDefault(Lane defaultLane) {
        return targetLane == null ? defaultLane : targetLane;
    }
}
