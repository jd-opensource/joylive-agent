package com.jd.live.agent.governance.invoke.metadata.parser;

import com.jd.live.agent.core.Constants;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.instance.Location;
import com.jd.live.agent.core.util.matcher.Matcher;
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
        Location location = application.getLocation();

        // target space
        String targetSpaceId = parseLaneSpace();
        LaneSpace targetSpace = governancePolicy == null ? null : governancePolicy.getLaneSpace(targetSpaceId);
        targetSpaceId = targetSpace == null ? targetSpaceId : targetSpace.getId();

        // target lane
        String targetLaneId = parseLane(targetSpaceId, targetSpace);
        Lane targetLane = targetSpace == null ? null : targetSpace.getLane(targetLaneId);
        targetLaneId = targetLane == null ? targetLaneId : targetLane.getCode();

        // local space
        String localSpaceId = location.getLaneSpaceId();
        LaneSpace localSpace = governancePolicy == null ? null : governancePolicy.getLaneSpace(localSpaceId);
        localSpaceId = localSpace == null ? localSpaceId : localSpace.getId();

        // local lane
        String localLaneId = location.getLane();
        Lane localLane = localSpace == null ? null : localSpace.getLane(localLaneId);
        localLaneId = localLane == null ? localLaneId : localLane.getCode();

        // default space
        LaneSpace defaultSpace = governancePolicy == null ? null : governancePolicy.getDefaultLaneSpace();
        String defaultSpaceId = defaultSpace == null ? null : defaultSpace.getId();

        // default lane
        Lane defaultLane = defaultSpace == null ? null : defaultSpace.getDefaultLane();
        String defaultLaneId = defaultLane == null ? null : defaultLane.getCode();

        LaneMetadata metadata = LaneMetadata.builder()
                .laneConfig(laneConfig)
                .targetSpaceId(targetSpaceId)
                .targetSpace(targetSpace)
                .targetLaneId(targetLaneId)
                .targetLane(targetLane)
                .localSpaceId(localSpaceId)
                .localSpace(localSpace)
                .localLaneId(localLaneId)
                .localLane(localLane)
                .defaultSpaceId(defaultSpaceId)
                .defaultSpace(defaultSpace)
                .defaultLaneId(defaultLaneId)
                .defaultLane(defaultLane)
                .build();
        inject(metadata);
        return metadata;
    }

    /**
     * Parses the lane space based on the lane configuration and application location.
     *
     * @return The lane space id, or null if not found.
     */
    protected String parseLaneSpace() {
        Cargo cargo = RequestContext.getCargo(Constants.LABEL_LANE_SPACE_ID);
        String laneSpaceId = cargo == null ? null : cargo.getFirstValue();
        if ((laneSpaceId == null || laneSpaceId.isEmpty()) && laneConfig.isFallbackLocationIfNoSpace()) {
            laneSpaceId = application.getLocation().getLaneSpaceId();
        }
        return laneSpaceId;
    }

    /**
     * Parses the lane code from the request context.
     *
     * @param laneSpaceId  The lane space id.
     * @param laneSpace    The lane space.
     * @return The lane code as a String, or null if not found.
     */
    protected String parseLane(String laneSpaceId, LaneSpace laneSpace) {
        Cargo cargo = RequestContext.getCargo(Constants.LABEL_LANE);
        String lane = cargo == null ? null : cargo.getFirstValue();
        Location location = application.getLocation();
        if ((lane == null || lane.isEmpty())
                && laneSpaceId != null
                && laneSpaceId.equals(location.getLaneSpaceId())
                && laneConfig.isFallbackLocationIfNoSpace()) {
            lane = location.getLane();
        }
        return lane;
    }

    /**
     * Injects the lane metadata into the current request context, updating the carrier with space ID and lane code.
     *
     * @param metadata the lane metadata to inject into the request context
     */
    protected void inject(LaneMetadata metadata) {
        Carrier carrier = RequestContext.getOrCreate();
        if (metadata.getTargetSpaceId() != null
                && !metadata.getTargetSpaceId().isEmpty()) {
            addCargo(carrier, metadata);
        } else {
            removeCargo(carrier);
        }
    }

    /**
     * Adds Lane Space ID and Lane ID to the given Carrier object.
     *
     * @param carrier  the Carrier object to add cargo to
     * @param metadata the LaneMetadata containing the target Lane Space ID and Lane ID
     */
    protected void addCargo(Carrier carrier, LaneMetadata metadata) {
        if (carrier.getCargo(Constants.LABEL_LANE_SPACE_ID) == null) {
            carrier.setCargo(Constants.LABEL_LANE_SPACE_ID, metadata.getTargetSpaceId());
            carrier.setCargo(Constants.LABEL_LANE, metadata.getTargetLaneId());
        }
    }

    /**
     * Removes Lane Space ID and Lane ID from the given Carrier object.
     *
     * @param carrier the Carrier object to remove cargo from
     */
    protected void removeCargo(Carrier carrier) {
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
        protected String parseLaneSpace() {
            if (domainPolicy != null) {
                LaneSpace laneSpace = domainPolicy.getLaneSpace();
                if (laneSpace != null) {
                    return laneSpace.getId();
                }
            }
            return fallbackLaneSpace();
        }

        @Override
        protected String parseLane(String laneSpaceId, LaneSpace laneSpace) {
            if (laneSpace != null) {
                LaneRule laneRule = getLaneRule(laneSpace);
                if (laneRule != null) {
                    if (laneRule.match(matcher)) {
                        return laneRule.getLaneCode();
                    }
                }
            }
            return fallbackLane(laneSpaceId, laneSpace);
        }

        /**
         * Fallback method to parse lane space if the default implementation fails.
         *
         * @return the parsed lane space
         */
        protected String fallbackLaneSpace() {
            return super.parseLaneSpace();
        }

        /**
         * Fallback method to parse lane if the default implementation fails.
         *
         * @param laneSpaceId the ID of the lane space
         * @param laneSpace   the lane space object
         * @return the parsed lane
         */
        protected String fallbackLane(String laneSpaceId, LaneSpace laneSpace) {
            return super.parseLane(laneSpaceId, laneSpace);
        }

        /**
         * Retrieves the LaneRule associated with a given LaneSpace based on the LaneDomain policy.
         *
         * @param laneSpace The LaneSpace for which to retrieve the LaneRule
         * @return The LaneRule associated with the LaneSpace, or null if no rule applies
         */
        protected LaneRule getLaneRule(LaneSpace laneSpace) {
            LaneDomain domain = domainPolicy == null ? null : domainPolicy.getLaneDomain();
            LaneRule rule = null;
            if (domain == null) {
                if (laneSpace.getDomainSize() == 0 && laneSpace.getRuleSize() == 1) {
                    rule = laneSpace.getRules().get(0);
                }
            } else {
                LanePath path = domain.getPath(request.getPath());
                if (path != null) {
                    rule = laneSpace.getLaneRule(path.getRuleId());
                } else if (domain.getPathSize() == 0 && laneSpace.getRuleSize() == 1) {
                    rule = laneSpace.getRules().get(0);
                }
            }
            return rule;
        }

    }

    /**
     * A parser for gateway inbound lane metadata.
     */
    public static class GatewayInboundLaneMetadataParser extends HttpInboundLaneMetadataParser {


        public GatewayInboundLaneMetadataParser(ServiceRequest request,
                                                LaneConfig laneConfig,
                                                Application application,
                                                GovernancePolicy governancePolicy,
                                                DomainPolicy domainPolicy,
                                                Matcher<TagCondition> matcher) {
            super(request, laneConfig, application, governancePolicy, domainPolicy, matcher);
        }

        @Override
        protected String fallbackLaneSpace() {
            if (application.getService().isFrontGateway()) {
                return laneConfig.isFallbackLocationIfNoSpace() ? application.getLocation().getLaneSpaceId() : null;
            } else {
                return super.fallbackLaneSpace();
            }
        }

        @Override
        protected String fallbackLane(String laneSpaceId, LaneSpace laneSpace) {
            if (application.getService().isFrontGateway()) {
                Location location = application.getLocation();
                return laneSpaceId != null && laneSpaceId.equals(location.getLaneSpaceId()) ? location.getLane() : null;
            }
            return super.fallbackLane(laneSpaceId, laneSpace);
        }

        @Override
        protected void removeCargo(Carrier carrier) {
            if (application.getService().isFrontGateway()) {
                carrier.removeCargo(Constants.LABEL_LANE_SPACE_ID);
                carrier.removeCargo(Constants.LABEL_LANE);
            } else {
                super.removeCargo(carrier);
            }
        }

        @Override
        protected void addCargo(Carrier carrier, LaneMetadata metadata) {
            if (application.getService().isFrontGateway()) {
                carrier.setCargo(Constants.LABEL_LANE_SPACE_ID, metadata.getTargetSpaceId());
                carrier.setCargo(Constants.LABEL_LANE, metadata.getTargetLaneId());
            } else {
                super.addCargo(carrier, metadata);
            }
        }
    }
}

