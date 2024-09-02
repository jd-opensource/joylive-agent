package com.jd.live.agent.governance.invoke.metadata.parser;

import com.jd.live.agent.core.Constants;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.util.matcher.Matcher;
import com.jd.live.agent.core.util.option.Converts;
import com.jd.live.agent.governance.config.LaneConfig;
import com.jd.live.agent.governance.context.RequestContext;
import com.jd.live.agent.governance.context.bag.Cargo;
import com.jd.live.agent.governance.context.bag.Carrier;
import com.jd.live.agent.governance.invoke.metadata.LaneMetadata;
import com.jd.live.agent.governance.invoke.metadata.parser.MetadataParser.LaneParser;
import com.jd.live.agent.governance.policy.GovernancePolicy;
import com.jd.live.agent.governance.policy.domain.DomainPolicy;
import com.jd.live.agent.governance.policy.lane.*;
import com.jd.live.agent.governance.request.ServiceRequest;
import com.jd.live.agent.governance.rule.tag.TagCondition;
import com.jd.live.agent.governance.rule.tag.TagGroup;

import java.util.Map;

/**
 * The {@code LaneMetadataParser} class is responsible for parsing metadata related to lanes,
 * which are specific paths or channels within a service. This parser implements the
 * {@code MetadataParser.LaneParser} interface to provide lane-specific metadata.
 */
public class LaneMetadataParser implements LaneParser {

    /**
     * the service request containing information about the lane
     */
    protected final ServiceRequest request;
    /**
     * the configuration for the lane
     */
    protected final LaneConfig laneConfig;
    /**
     * the application context
     */
    protected final Application application;
    /**
     * the governance policy that may affect lane parsing
     */
    protected final GovernancePolicy governancePolicy;

    /**
     * Constructs a new {@code LaneMetadataParser} with the specified parameters.
     *
     * @param request          the service request containing information about the lane
     * @param laneConfig       the configuration for the lane
     * @param application      the application context
     * @param governancePolicy the governance policy that may affect lane parsing
     */
    public LaneMetadataParser(ServiceRequest request, LaneConfig laneConfig,
                              Application application, GovernancePolicy governancePolicy) {
        this.request = request;
        this.laneConfig = laneConfig;
        this.application = application;
        this.governancePolicy = governancePolicy;
    }

    /**
     * Parses and constructs the lane metadata based on the request, configuration, and policy.
     *
     * @return a {@code LaneMetadata} object representing the parsed lane information
     */
    @Override
    public LaneMetadata parse() {
        LaneSpace laneSpace = parseLaneSpace();
        Lane currentLane = laneSpace == null ? null : laneSpace.getLane(application.getLocation().getLane());
        Lane targetLane = laneSpace == null ? null : laneSpace.getLane(parseLane(laneSpace));
        targetLane = targetLane == null && laneSpace != null ? laneSpace.getDefaultLane() : targetLane;
        return LaneMetadata.builder()
                .laneConfig(laneConfig)
                .laneSpace(laneSpace)
                .currentLane(currentLane)
                .targetLane(targetLane).build();
    }

    /**
     * Parses the lane space based on the lane configuration and application location.
     *
     * @return The lane space object, or null if not found.
     */
    protected LaneSpace parseLaneSpace() {
        Cargo cargo = RequestContext.getCargo(Constants.LABEL_LANE_SPACE_ID);
        String laneSpaceId = cargo == null ? null : Converts.getString(cargo.getFirstValue());
        laneSpaceId = laneSpaceId != null ? laneSpaceId : application.getLocation().getLaneSpaceId();
        return governancePolicy == null ? null : governancePolicy.getLaneSpace(laneSpaceId);
    }

    /**
     * Parses the lane code from the request context.
     *
     * @param laneSpace lane space
     * @return The lane code as a String, or null if not found.
     */
    protected String parseLane(LaneSpace laneSpace) {
        Cargo cargo = RequestContext.getCargo(Constants.LABEL_LANE);
        return cargo == null ? null : cargo.getFirstValue();
    }

    /**
     * A specialized parser for inbound lane metadata. This class extends the {@link LaneMetadataParser}
     * to provide functionality specific to parsing metadata for inbound lanes in a service request
     * context. It is designed to handle the extraction and interpretation of metadata that is relevant
     * to the processing of incoming service requests on a particular lane.
     */
    public static class InboundLaneMetadataParser extends LaneMetadataParser {

        public InboundLaneMetadataParser(ServiceRequest request,
                                         LaneConfig laneConfig,
                                         Application application,
                                         GovernancePolicy governancePolicy) {
            super(request, laneConfig, application, governancePolicy);
        }
    }

    /**
     * A specialized parser for HTTP inbound lane metadata. This class extends the {@link LaneMetadataParser}
     * to provide additional logic for parsing lane metadata in the context of domain policies and tag conditions.
     */
    public static class HttpInboundLaneMetadataParser extends InboundLaneMetadataParser {

        /**
         * The domain policy to be used for parsing lane metadata.
         */
        protected DomainPolicy domainPolicy;

        /**
         * The matcher for tag conditions, used to determine if a lane rule applies.
         */
        protected Matcher<TagCondition> matcher;

        /**
         * Constructs a new HttpInboundLaneMetadataParser with the provided parameters.
         *
         * @param request          the service request for which lane metadata is being parsed
         * @param laneConfig       the lane configuration to use for parsing
         * @param application      the application context in which the parsing is being done
         * @param governancePolicy the governance policy to apply during parsing
         * @param domainPolicy     the domain policy to use for additional lane metadata resolution
         * @param matcher          the matcher for evaluating tag conditions against lane rules
         */
        public HttpInboundLaneMetadataParser(ServiceRequest request,
                                             LaneConfig laneConfig,
                                             Application application,
                                             GovernancePolicy governancePolicy,
                                             DomainPolicy domainPolicy,
                                             Matcher<TagCondition> matcher) {
            super(request, laneConfig, application, governancePolicy);
            this.domainPolicy = domainPolicy;
            this.matcher = matcher;
        }

        @Override
        public LaneMetadata parse() {
            LaneMetadata result = super.parse();
            if (domainPolicy != null) {
                inject(result);
            }
            return result;
        }

        @Override
        protected LaneSpace parseLaneSpace() {
            return domainPolicy != null ? domainPolicy.getLaneSpace() : super.parseLaneSpace();
        }

        @Override
        protected String parseLane(LaneSpace laneSpace) {
            if (domainPolicy == null) {
                return super.parseLane(laneSpace);
            }
            LaneDomain laneDomain = domainPolicy.getLaneDomain();
            LanePath lanePath = laneDomain == null ? null : laneDomain.getPath(request.getPath());
            if (lanePath != null) {
                LaneRule laneRule = laneSpace.getLaneRule(lanePath.getRuleId());
                Map<String, TagGroup> conditions = laneRule == null ? null : laneRule.getConditions();
                if (conditions != null) {
                    for (Map.Entry<String, TagGroup> entry : conditions.entrySet()) {
                        if (entry.getValue().match(matcher)) {
                            return entry.getKey();
                        }
                    }
                }
            }
            return null;
        }

        /**
         * Injects the lane metadata into the current request context, updating the carrier with space ID and lane code.
         *
         * @param metadata the lane metadata to inject into the request context
         */
        protected void inject(LaneMetadata metadata) {
            LaneSpace laneSpace = metadata.getLaneSpace();
            Lane targetLane = metadata.getTargetLane();
            Carrier carrier = RequestContext.getOrCreate();
            if (null != targetLane) {
                carrier.setCargo(Constants.LABEL_LANE_SPACE_ID, laneSpace.getId());
                carrier.setCargo(Constants.LABEL_LANE, targetLane.getCode());
            } else {
                carrier.removeCargo(Constants.LABEL_LANE_SPACE_ID);
                carrier.removeCargo(Constants.LABEL_LANE);
            }
        }
    }
}

