package com.jd.live.agent.governance.invoke.metadata;

import com.jd.live.agent.core.instance.Location;
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
     * The current unit for this invocation.
     */
    private Unit currentUnit;

    /**
     * The current cell for this invocation.
     */
    private Cell currentCell;

    /**
     * The center unit for this invocation.
     */
    private Unit centerUnit;

    /**
     * The live space ID for this invocation.
     */
    private String liveSpaceId;

    /**
     * The unit rule ID for this invocation.
     */
    private String unitRuleId;

    /**
     * The unit rule for this invocation.
     */
    private UnitRule unitRule;

    /**
     * The variable for this invocation.
     */
    private String variable;

    public String getLiveSpaceId() {
        return liveSpace == null ? null : liveSpace.getId();
    }

    /**
     * Checks if the given location matches the current live space ID.
     *
     * @param location the location to check
     * @return true if the location matches the current live space ID, false otherwise
     */
    public boolean match(Location location) {
        String current = location.getLiveSpaceId();
        return current == null || current.isEmpty()
                || liveSpaceId == null || liveSpaceId.isEmpty()
                || current.equals(liveSpaceId);
    }

    private static final class LiveMetadataBuilderImpl extends LiveMetadataBuilder<LiveMetadata, LiveMetadataBuilderImpl> {
    }

    public abstract static class LiveMetadataBuilder<C extends LiveMetadata, B extends LiveMetadataBuilder<C, B>> {
    }
}
