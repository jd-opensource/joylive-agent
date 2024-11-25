package com.jd.live.agent.governance.invoke.metadata.parser;

import com.jd.live.agent.core.Constants;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.instance.Location;
import com.jd.live.agent.governance.config.LiveConfig;
import com.jd.live.agent.governance.context.RequestContext;
import com.jd.live.agent.governance.context.bag.Cargo;
import com.jd.live.agent.governance.context.bag.Carrier;
import com.jd.live.agent.governance.invoke.metadata.LiveDomainMetadata;
import com.jd.live.agent.governance.invoke.metadata.LiveDomainMetadata.LiveDomainMetadataBuilder;
import com.jd.live.agent.governance.invoke.metadata.LiveMetadata;
import com.jd.live.agent.governance.invoke.metadata.LiveMetadata.LiveMetadataBuilder;
import com.jd.live.agent.governance.invoke.metadata.parser.MetadataParser.LiveParser;
import com.jd.live.agent.governance.policy.GovernancePolicy;
import com.jd.live.agent.governance.policy.PolicyId;
import com.jd.live.agent.governance.policy.domain.DomainPolicy;
import com.jd.live.agent.governance.policy.live.*;
import com.jd.live.agent.governance.policy.service.ServicePolicy;
import com.jd.live.agent.governance.policy.service.live.ServiceLivePolicy;
import com.jd.live.agent.governance.policy.variable.VariableFunction;
import com.jd.live.agent.governance.policy.variable.VariableParser;
import com.jd.live.agent.governance.policy.variable.VariableSource.ExpressionVariable;
import com.jd.live.agent.governance.policy.variable.VariableSource.ExpressionVariableSource;
import com.jd.live.agent.governance.policy.variable.VariableSource.HttpVariable;
import com.jd.live.agent.governance.policy.variable.VariableSource.HttpVariableSource;
import com.jd.live.agent.governance.request.HttpRequest;
import com.jd.live.agent.governance.request.RpcRequest;
import com.jd.live.agent.governance.request.ServiceRequest;

import java.util.function.Function;

/**
 * The {@code LiveMetadataParser} class is responsible for parsing metadata that is specific
 * to live services within an application. It implements the {@code MetadataParser.LiveParser}
 * interface to provide live-specific metadata parsing functionality.
 */
public abstract class LiveMetadataParser implements LiveParser {

    /**
     * the service request containing information about the live service
     */
    protected final ServiceRequest request;
    /**
     * the configuration for the live service
     */
    protected final LiveConfig liveConfig;
    /**
     * the application context
     */
    protected final Application application;
    /**
     * the governance policy that may affect live metadata parsing
     */
    protected final GovernancePolicy governancePolicy;

    /**
     * Constructs a new {@code LiveMetadataParser} with the specified parameters.
     *
     * @param request          the service request containing information about the live service
     * @param liveConfig       the configuration for the live service
     * @param application      the application context
     * @param governancePolicy the governance policy that may affect live metadata parsing
     */
    public LiveMetadataParser(ServiceRequest request,
                              LiveConfig liveConfig,
                              Application application,
                              GovernancePolicy governancePolicy) {
        this.request = request;
        this.liveConfig = liveConfig;
        this.application = application;
        this.governancePolicy = governancePolicy;
    }

    @Override
    public LiveMetadata parse() {
        return configure(LiveMetadata.builder()).build();
    }

    /**
     * Configures the provided {@link LiveMetadataBuilder} with the relevant live metadata information
     * extracted from the current request and application context. This method populates the builder
     * with details such as the live space, current unit and cell, center unit, unit rule, and variable.
     *
     * @param builder The live metadata builder to configure.
     * @return The configured live metadata builder.
     */
    protected LiveMetadataBuilder<?, ?> configure(LiveMetadataBuilder<?, ?> builder) {
        String targetSpaceId = parseLiveSpaceId();
        LiveSpace targetLiveSpace = parseLiveSpace(targetSpaceId);
        String unitRuleId = parseRuleId(targetSpaceId);
        UnitRule unitRule = targetLiveSpace == null || unitRuleId == null ? null : targetLiveSpace.getUnitRule(unitRuleId);
        String variable = parseVariable();
        String localSpaceId = application.getLocation().getLiveSpaceId();
        LiveSpace localSpace = governancePolicy == null ? null : governancePolicy.getLocalLiveSpace();
        builder.liveConfig(liveConfig).
                localSpaceId(localSpaceId).
                localSpace(localSpace).
                targetSpaceId(targetSpaceId).
                targetSpace(targetLiveSpace).
                ruleId(unitRuleId).
                rule(unitRule).
                variable(variable);
        return builder;
    }

    /**
     * Parses the live space with the given space ID.
     *
     * @param spaceId the ID of the live space to parse
     * @return the parsed live space, or null if the governance policy is null or the live space with the given ID does not exist
     */
    protected LiveSpace parseLiveSpace(String spaceId) {
        return governancePolicy == null ? null : governancePolicy.getLiveSpace(spaceId);
    }

    /**
     * Parses the live space ID from the request context using a specified key.
     *
     * @return The parsed live space ID as a Long, or null if the live space ID is not found.
     */
    protected String parseLiveSpaceId() {
        String result = null;
        Cargo cargo = RequestContext.getCargo(Constants.LABEL_LIVE_SPACE_ID);
        if (cargo != null) {
            result = cargo.getFirstValue();
        } else if (isFallbackLocationIfNoSpace()) {
            result = application.getLocation().getLiveSpaceId();
            if (result != null && !result.isEmpty()) {
                RequestContext.getOrCreate().addCargo(Constants.LABEL_LIVE_SPACE_ID, result);
            }
        }
        return result;
    }

    /**
     * Parses the rule ID from the request context using a specified key.
     *
     * @return The parsed rule ID as a Long, or null if the rule ID is not found.
     */
    protected String parseRuleId(String spaceId) {
        String result = null;
        Cargo cargo = RequestContext.getCargo(Constants.LABEL_RULE_ID);
        if (cargo != null) {
            result = cargo.getFirstValue();
        } else if (spaceId != null && !spaceId.isEmpty() && isFallbackLocationIfNoSpace()) {
            Location location = application.getLocation();
            if (spaceId.equals(location.getLiveSpaceId())) {
                result = location.getUnitRuleId();
                if (result != null && !result.isEmpty()) {
                    RequestContext.getOrCreate().addCargo(Constants.LABEL_RULE_ID, result);
                }
            }
        }
        return result;
    }

    /**
     * Checks if the fallback location should be used when there is no space available.
     *
     * @return true if the fallback location should be used when there is no space available, false otherwise
     */
    protected boolean isFallbackLocationIfNoSpace() {
        return liveConfig.isFallbackLocationIfNoSpace();
    }

    /**
     * Parses the variable from the request or context.
     *
     * @return The first value associated with the cargo for the specified variable key, or {@code null} if no such value exists.
     */
    protected String parseVariable() {
        Cargo cargo = RequestContext.getCargo(Constants.LABEL_VARIABLE);
        return cargo == null ? null : cargo.getFirstValue();
    }

    /**
     * A parser for inbound live metadata.
     */
    public static class InboundLiveMetadataParser extends LiveMetadataParser {

        /**
         * Constructs a new {@code InboundLiveMetadataParser} with the specified parameters.
         *
         * @param request          the service request containing information about the live service
         * @param liveConfig       the configuration for the live service
         * @param application      the application context
         * @param governancePolicy the governance policy that may affect live metadata parsing
         */
        public InboundLiveMetadataParser(ServiceRequest request, LiveConfig liveConfig, Application application, GovernancePolicy governancePolicy) {
            super(request, liveConfig, application, governancePolicy);
        }

    }

    /**
     * A parser for HTTP inbound live metadata. This class extends the {@link LiveMetadataParser} to
     * handle the parsing of live metadata from incoming HTTP requests. It includes additional fields
     * and logic to process variable parsers, variable functions, and domain policies, which are
     * specific to the HTTP context.
     *
     * <p>This parser is responsible for extracting and transforming metadata from HTTP requests,
     * applying domain policies, and generating the appropriate live metadata objects that can be
     * used by the application for further processing.</p>
     */
    public static class HttpInboundLiveMetadataParser extends LiveMetadataParser {
        /**
         * A map of variable parsers used to parse specific variables from HTTP requests.
         */
        protected Function<String, VariableParser<?, ?>> variableParsers;
        /**
         * A map of variable functions used to process variables after parsing.
         */
        protected Function<String, VariableFunction> variableFunctions;
        /**
         * The domain policy that governs the parsing and handling of live metadata.
         */
        protected DomainPolicy domainPolicy;

        /**
         * Constructs a new HttpInboundLiveMetadataParser with the provided request, live configuration,
         * application context, governance policy, variable parsers, variable functions, and domain policy.
         *
         * @param request           The service request for which to parse live metadata.
         * @param liveConfig        The live configuration containing relevant settings.
         * @param application       The application context that provides additional information.
         * @param governancePolicy  The governance policy that must be adhered to.
         * @param variableParsers   A map of variable parsers used to parse specific variables.
         * @param variableFunctions A map of variable functions used to process variables.
         * @param domainPolicy      The domain policy that governs the parsing and handling of live metadata.
         */
        public HttpInboundLiveMetadataParser(ServiceRequest request,
                                             LiveConfig liveConfig,
                                             Application application,
                                             GovernancePolicy governancePolicy,
                                             Function<String, VariableParser<?, ?>> variableParsers,
                                             Function<String, VariableFunction> variableFunctions,
                                             DomainPolicy domainPolicy) {
            super(request, liveConfig, application, governancePolicy);
            this.domainPolicy = domainPolicy;
            this.variableParsers = variableParsers;
            this.variableFunctions = variableFunctions;
        }

        @Override
        public LiveMetadata parse() {
            LiveDomainMetadata result = configure(LiveDomainMetadata.builder()).build();
            if (domainPolicy != null) {
                inject(result);
            }
            return result;
        }

        /**
         * Configures the live domain metadata builder with the parsed information from the HTTP request.
         * This method includes logic for applying domain policies, parsing variables, and setting up
         * the request context carrier with the appropriate metadata.
         *
         * @param builder The live domain metadata builder to configure.
         * @return The configured live domain metadata builder.
         */
        @SuppressWarnings("unchecked")
        protected LiveDomainMetadataBuilder<?, ?> configure(LiveDomainMetadataBuilder<?, ?> builder) {
            LiveSpace liveSpace = domainPolicy == null ? null : domainPolicy.getLiveSpace();
            if (liveSpace == null) {
                return configureLivelessDomain(builder);
            }
            Unit localUnit = liveSpace.getLocalUnit();
            LiveDomain liveDomain = domainPolicy.getLiveDomain();
            UnitDomain unitDomain = domainPolicy.isUnit() ? domainPolicy.getUnitDomain() :
                    (localUnit == null ? null : liveDomain.getUnitDomain(localUnit.getCode()));
            String host = liveDomain.getHost();
            String unitHost = unitDomain == null ? null : unitDomain.getHost();
            String unitBackend = unitDomain == null ? null : unitDomain.getBackend();
            LivePath path = liveDomain.getPath(request.getPath());
            String unitPath = path == null ? null : path.getPath();

            VariableParser<HttpRequest, HttpVariableSource> parser =
                    (VariableParser<HttpRequest, HttpVariableSource>) variableParsers.apply(VariableParser.TYPE_HTTP);
            String bizVariable = path == null || !path.isBizVariableEnabled() || parser == null ? null :
                    parser.parse((HttpRequest) request, new HttpVariable(path.getBizVariableScope(), path.getBizVariableName()));
            LiveVariableRule variableRule = bizVariable == null ? null : path.getVariableRule(bizVariable);
            String variableName = null;
            String sourceName = null;
            PolicyId policyId = null;
            String unitRuleId = null;
            if (path != null) {
                policyId = variableRule != null ? variableRule : path;
                unitRuleId = variableRule != null ? variableRule.getRuleId() : path.getRuleId();
                variableName = path.isCustomVariableSource() ? path.getVariable() : variableName;
                sourceName = path.isCustomVariableSource() ? path.getVariableSource() : sourceName;
            }
            UnitRule unitRule = unitRuleId == null ? null : liveSpace.getUnitRule(unitRuleId);
            Carrier carrier = RequestContext.getOrCreate();
            // The gateway may have decrypted the user and placed it in the context attribute
            String variable = carrier.getAttribute(Constants.LABEL_VARIABLE);
            if (variable == null) {
                // Parse variables according to rules
                variableName = variableName == null && unitRule != null ? unitRule.getVariable() : variableName;
                sourceName = sourceName == null && unitRule != null ? unitRule.getVariableSource() : sourceName;
                LiveVariable liveVariable = variableName == null ? null : liveSpace.getVariable(variableName);
                LiveVariableSource variableSource = liveVariable == null || sourceName == null ? null : liveVariable.getSource(sourceName);
                VariableFunction variableFunction = variableSource == null ? null : variableFunctions.apply(variableSource.getFunc());
                variable = parser == null ? null : parser.parse((HttpRequest) request, variableSource, variableFunction);
            }
            String localSpaceId = application.getLocation().getLiveSpaceId();
            LiveSpace localSpace = governancePolicy == null ? null : governancePolicy.getLocalLiveSpace();
            return builder.liveConfig(liveConfig).
                    localSpaceId(localSpaceId).
                    localSpace(localSpace).
                    targetSpaceId(liveSpace.getId()).
                    targetSpace(liveSpace).
                    host(host).
                    unitHost(unitHost).
                    unitBackend(unitBackend).
                    unitPath(unitPath).
                    ruleId(unitRuleId).
                    rule(unitRule).
                    bizVariable(bizVariable).
                    variable(variable).
                    policyId(policyId);
        }

        protected LiveDomainMetadataBuilder<?, ?> configureLivelessDomain(LiveDomainMetadataBuilder<?, ?> builder) {
            return (LiveDomainMetadataBuilder<?, ?>) super.configure(builder);
        }

        /**
         * Injects the provided live domain metadata into the current request context. This method
         * updates the contextual tags in the request with the information from the live domain metadata,
         * such as the live space ID, unit rule ID, and variable data. If no unit rule is present in the
         * metadata, the corresponding contextual tags are removed from the request context.
         *
         * <p>This method is typically called after the metadata has been parsed and is ready to be used
         * by the application for further processing. It ensures that the necessary metadata is available
         * throughout the request handling lifecycle.</p>
         *
         * @param metadata The live domain metadata to inject into the request context.
         */
        protected void inject(LiveDomainMetadata metadata) {
            // Overwrite contextual tags
            LiveSpace liveSpace = metadata.getTargetSpace();
            UnitRule unitRule = metadata.getRule();
            Carrier carrier = RequestContext.getOrCreate();
            if (unitRule != null) {
                carrier.setCargo(Constants.LABEL_LIVE_SPACE_ID, liveSpace.getId());
                carrier.setCargo(Constants.LABEL_RULE_ID, unitRule.getId());
                carrier.setCargo(Constants.LABEL_VARIABLE, metadata.getVariable());
            } else {
                carrier.removeCargo(Constants.LABEL_LIVE_SPACE_ID);
                carrier.removeCargo(Constants.LABEL_RULE_ID);
                carrier.removeCargo(Constants.LABEL_VARIABLE);
            }
        }
    }

    /**
     * A parser for gateway inbound live metadata.
     * <p>
     * This class extends the {@link HttpInboundLiveMetadataParser} and provides specific parsing logic for gateway inbound live metadata.
     */
    public static class GatewayInboundLiveMetadataParser extends HttpInboundLiveMetadataParser {

        /**
         * Constructs a new GatewayInboundLiveMetadataParser with the provided request, live configuration,
         * application context, governance policy, variable parsers, variable functions, and domain policy.
         *
         * @param request           The service request for which to parse live metadata.
         * @param liveConfig        The live configuration containing relevant settings.
         * @param application       The application context that provides additional information.
         * @param governancePolicy  The governance policy that must be adhered to.
         * @param variableParsers   A map of variable parsers used to parse specific variables.
         * @param variableFunctions A map of variable functions used to process variables.
         * @param domainPolicy      The domain policy that governs the parsing and handling of live metadata.
         */
        public GatewayInboundLiveMetadataParser(ServiceRequest request,
                                                LiveConfig liveConfig,
                                                Application application,
                                                GovernancePolicy governancePolicy,
                                                Function<String, VariableParser<?, ?>> variableParsers,
                                                Function<String, VariableFunction> variableFunctions,
                                                DomainPolicy domainPolicy) {
            super(request, liveConfig, application, governancePolicy, variableParsers, variableFunctions, domainPolicy);
        }

        @Override
        protected LiveDomainMetadataBuilder<?, ?> configureLivelessDomain(LiveDomainMetadataBuilder<?, ?> builder) {
            if (application.getService().isFrontGateway()) {
                return builder;
            } else {
                return super.configureLivelessDomain(builder);
            }
        }
    }

    /**
     * A specialized parser for outbound live metadata. This class is designed to parse and prepare
     * live metadata for outgoing service requests. It extends the base {@link LiveMetadataParser}
     * class to inherit common parsing functionality and adds additional logic specific to outbound
     * requests.
     *
     * <p>Outbound live metadata includes information that needs to be sent along with the request to
     * the service, such as the current live space, unit, cell, and governance policy.</p>
     */
    public static class OutboundLiveMetadataParser extends LiveMetadataParser {

        public OutboundLiveMetadataParser(ServiceRequest request,
                                          LiveConfig liveConfig,
                                          Application application,
                                          GovernancePolicy governancePolicy) {
            super(request, liveConfig, application, governancePolicy);
        }

        @Override
        protected String parseRuleId(String spaceId) {
            // TODO get target service rule.
            return super.parseRuleId(spaceId);
        }

        @Override
        protected boolean isFallbackLocationIfNoSpace() {
            return true;
        }
    }

    /**
     * A parser for RPC outbound live metadata. This class extends the {@link OutboundLiveMetadataParser}
     * to provide additional functionality specific to RPC (Remote Procedure Call) requests. It includes
     * a map of variable parsers that can be used to parse and process different types of variables
     * required for the RPC live metadata.
     *
     * <p>RPC live metadata often requires parsing and transforming variables according to the specific
     * requirements of the RPC protocol. This class allows for such custom variable parsing.</p>
     */
    public static class RpcOutboundLiveMetadataParser extends OutboundLiveMetadataParser {

        /**
         * A map of variable parsers used to parse specific variables for RPC live metadata.
         */
        protected final Function<String, VariableParser<?, ?>> variableParsers;

        public RpcOutboundLiveMetadataParser(ServiceRequest request,
                                             LiveConfig liveConfig,
                                             Application application,
                                             GovernancePolicy governancePolicy,
                                             Function<String, VariableParser<?, ?>> variableParsers) {

            super(request, liveConfig, application, governancePolicy);
            this.variableParsers = variableParsers;
        }

        @SuppressWarnings("unchecked")
        @Override
        public LiveMetadata configure(LiveMetadata metadata, ServicePolicy servicePolicy) {
            ServiceLivePolicy livePolicy = servicePolicy == null ? null : servicePolicy.getLivePolicy();
            String variableExpression = livePolicy == null ? null : livePolicy.getVariableExpression();
            if (variableExpression != null && !variableExpression.isEmpty()) {
                VariableParser<RpcRequest, ExpressionVariableSource> parser =
                        (VariableParser<RpcRequest, ExpressionVariableSource>) variableParsers.apply(VariableParser.TYPE_EXPRESSION);
                String variable = parser == null ? null : parser.parse((RpcRequest) request,
                        new ExpressionVariable(variableExpression));
                return LiveMetadata.builder().
                        liveConfig(metadata.getLiveConfig()).
                        localSpaceId(metadata.getLocalSpaceId()).
                        localSpace(metadata.getLocalSpace()).
                        targetSpaceId(metadata.getTargetSpaceId()).
                        targetSpace(metadata.getTargetSpace()).
                        ruleId(metadata.getRuleId()).
                        rule(metadata.getRule()).
                        variable(variable).
                        build();
            }
            return metadata;
        }
    }
}

