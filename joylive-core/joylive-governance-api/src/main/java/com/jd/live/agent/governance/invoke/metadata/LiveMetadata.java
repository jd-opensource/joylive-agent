package com.jd.live.agent.governance.invoke.metadata;

import com.jd.live.agent.core.util.URI;
import com.jd.live.agent.governance.config.LiveConfig;
import com.jd.live.agent.governance.event.TrafficEvent.TrafficEventBuilder;
import com.jd.live.agent.governance.policy.PolicyId;
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
     * The local live space id for this invocation.
     */
    private String localSpaceId;

    /**
     * The local live space for this invocation.
     */
    private LiveSpace localSpace;

    /**
     * The target live space ID for this invocation.
     */
    private String targetSpaceId;

    /**
     * The target live space for this invocation.
     */
    private LiveSpace targetSpace;

    /**
     * The unit rule ID for this invocation.
     */
    private String ruleId;

    /**
     * The unit rule for this invocation.
     */
    private UnitRule rule;

    /**
     * The variable for this invocation.
     */
    private String variable;

    /**
     * The policy identifier associated with the live domain.
     */
    private PolicyId policyId;

    public boolean isLocalLiveless() {
        return localSpace == null;
    }

    public Unit getLocalUnit() {
        return localSpace == null ? null : localSpace.getLocalUnit();
    }

    public Unit getLocalCenter() {
        return localSpace == null ? null : localSpace.getCenter();
    }

    public Cell getLocalCell() {
        return localSpace == null ? null : localSpace.getLocalCell();
    }

    public boolean isTargetLiveless() {
        return targetSpace == null;
    }

    public Unit getTargetLocalUnit() {
        return targetSpace == null ? null : targetSpace.getLocalUnit();
    }

    public Unit getTargetCenter() {
        return targetSpace == null ? null : targetSpace.getCenter();
    }

    public Cell getTargetLocalCell() {
        return targetSpace == null ? null : targetSpace.getLocalCell();
    }

    /**
     * Configures a live event builder with details from the current invocation context.
     *
     * @param builder The live event builder to configure.
     * @return The configured live event builder.
     */
    public TrafficEventBuilder configure(TrafficEventBuilder builder) {
        Unit localUnit = getLocalUnit();
        Cell localCell = getLocalCell();
        builder = builder.liveSpaceId(targetSpaceId)
                .unitRuleId(ruleId)
                .localUnit(localUnit == null ? null : localUnit.getCode())
                .localCell(localCell == null ? null : localCell.getCode())
                .liveVariable(variable);
        URI uri = policyId == null ? null : policyId.getUri();
        if (uri != null) {
            builder = builder.liveDomain(uri.getHost()).livePath(uri.getPath());
        }
        return builder;
    }

    private static final class LiveMetadataBuilderImpl extends LiveMetadataBuilder<LiveMetadata, LiveMetadataBuilderImpl> {
    }

    public abstract static class LiveMetadataBuilder<C extends LiveMetadata, B extends LiveMetadataBuilder<C, B>> {
    }
}
