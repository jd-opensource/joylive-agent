package com.jd.live.agent.governance.invoke.metadata;

import com.jd.live.agent.governance.config.LiveConfig;
import com.jd.live.agent.governance.policy.live.Cell;
import com.jd.live.agent.governance.policy.live.LiveSpace;
import com.jd.live.agent.governance.policy.live.Unit;
import com.jd.live.agent.governance.policy.live.UnitRule;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * The {@code LiveMetadata} class encapsulates the metadata for a live request.
 */
@Getter
@SuperBuilder
public class LiveMetadata {

    /**
     * The live configuration associated with this invocation.
     */
    private LiveConfig liveConfig;

    /**
     * The live space context for this invocation.
     */
    private LiveSpace liveSpace;

    /**
     * The current unit context for this invocation.
     */
    private Unit currentUnit;

    /**
     * The current cell context for this invocation.
     */
    private Cell currentCell;

    /**
     * The center unit context for this invocation.
     */
    private Unit centerUnit;

    /**
     * The unit rule ID applicable to this invocation.
     */
    private String unitRuleId;

    /**
     * The unit rule applicable to this invocation.
     */
    private UnitRule unitRule;

    /**
     * The variable for this invocation.
     */
    private String variable;

    public String getLiveSpaceId() {
        return liveSpace == null ? null : liveSpace.getId();
    }
}
