package com.jd.live.agent.governance.invoke.metadata.parser;

import com.jd.live.agent.core.Constants;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.util.URI;
import com.jd.live.agent.core.util.cache.LazyObject;
import com.jd.live.agent.core.util.http.HttpMethod;
import com.jd.live.agent.governance.config.ServiceConfig;
import com.jd.live.agent.governance.context.bag.Cargo;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.invoke.metadata.ServiceMetadata;
import com.jd.live.agent.governance.invoke.metadata.parser.MetadataParser.ServiceParser;
import com.jd.live.agent.governance.policy.GovernancePolicy;
import com.jd.live.agent.governance.policy.live.LiveType;
import com.jd.live.agent.governance.policy.live.UnitRule;
import com.jd.live.agent.governance.policy.service.Service;
import com.jd.live.agent.governance.policy.service.ServicePolicy;
import com.jd.live.agent.governance.policy.service.live.CellPolicy;
import com.jd.live.agent.governance.policy.service.live.ServiceLivePolicy;
import com.jd.live.agent.governance.policy.service.live.UnitPolicy;
import com.jd.live.agent.governance.request.HttpRequest;
import com.jd.live.agent.governance.request.ServiceRequest;

import java.util.HashMap;
import java.util.Map;

import static com.jd.live.agent.governance.policy.PolicyId.KEY_SERVICE_GROUP;
import static com.jd.live.agent.governance.policy.PolicyId.KEY_SERVICE_METHOD;

/**
 * The {@code AbstractServiceMetadataParser} class is responsible for parsing and constructing the metadata
 * for a service request based on the provided configuration, application context, and governance policy.
 * It implements the {@code MetadataParser} interface to produce a {@code ServiceMetadata} object.
 */
public abstract class ServiceMetadataParser implements ServiceParser {

    /**
     * The service request containing information about the service invocation.
     */
    protected final ServiceRequest request;

    /**
     * The governance policy which defines the rules for service interaction.
     */
    protected final GovernancePolicy policy;

    /**
     * The service configuration containing parameters and settings for the service.
     */
    protected final ServiceConfig config;

    /**
     * The application context which may provide additional service information.
     */
    protected final Application application;

    /**
     * A flag indicating whether live policy is enabled.
     */
    protected final boolean liveEnabled;

    /**
     * Constructs a new instance of {@code ServiceMetadataParser} with the specified parameters.
     *
     * @param request  the service request to be parsed
     * @param policy   the governance policy defining rules for service interaction
     * @param context  the invocation context providing additional information
     */
    public ServiceMetadataParser(ServiceRequest request, GovernancePolicy policy, InvocationContext context) {
        this.request = request;
        this.policy = policy;
        this.config = context.getGovernanceConfig().getServiceConfig();
        this.application = context.getApplication();
        this.liveEnabled = context.isLiveEnabled();
    }

    @Override
    public ServiceMetadata parse() {
        String serviceName = parseServiceName();
        String serviceGroup = parseServiceGroup(serviceName);
        Service service = parseService(serviceName);
        String path = service == null ? parsePath() : service.getServiceType().normalize(parsePath());
        String method = parseMethod();
        ServicePolicy servicePolicy = parseServicePolicy(service, serviceGroup, path, method);

        // improve performance by lazy loading consumer, writeProtect and uri
        LazyObject<String> consumer = LazyObject.of(this::parseConsumer);
        LazyObject<Boolean> writeProtect = LazyObject.of(() -> parseWriteProtect(servicePolicy));
        LazyObject<URI> uri = LazyObject.of(() -> {
            Map<String, String> parameters = new HashMap<>(4);
            parameters.put(KEY_SERVICE_METHOD, method);
            if (serviceGroup != null && !serviceGroup.isEmpty()) {
                parameters.put(KEY_SERVICE_GROUP, serviceGroup);
            }
            return new URI(null, serviceName, null, path, parameters);
        });

        return new ServiceMetadata(config, serviceName, serviceGroup, path, method, service, servicePolicy, consumer, writeProtect, uri);
    }

    /**
     * Parses and returns the consumer name from.
     *
     * @return the parsed consumer name
     */
    protected abstract String parseConsumer();

    /**
     * Parses and returns the service name from either the application context or the service request.
     *
     * @return the parsed service name
     */
    protected abstract String parseServiceName();

    /**
     * Parses and returns the service group from either the application context or the service request.
     *
     * @param serviceName the service name
     * @return the parsed service group
     */
    protected abstract String parseServiceGroup(String serviceName);

    /**
     * Parses and returns the method from the service request.
     *
     * @return the parsed method
     */
    protected String parseMethod() {
        return request.getMethod();
    }

    /**
     * Parses and returns the path from the service request.
     *
     * @return the parsed path
     */
    protected String parsePath() {
        return request.getPath();
    }

    /**
     * Parses and returns the service object from the governance policy using the service name.
     *
     * @param serviceName the name of the service to retrieve
     * @return the retrieved service object
     */
    protected Service parseService(String serviceName) {
        return policy == null ? null : policy.getService(serviceName);
    }

    /**
     * Parses and returns the service policy from the service object using the service group, path, and method.
     *
     * @param service      the service object to be queried
     * @param serviceGroup the service group to consider
     * @param path         the path to consider
     * @param method       the HTTP method to consider
     * @return the retrieved service policy or null if not found
     */
    protected ServicePolicy parseServicePolicy(Service service, String serviceGroup, String path, String method) {
        return service == null ? null : service.getPath(serviceGroup, path, method);
    }

    /**
     * Parses the write protection status from the given service policy.
     *
     * @param servicePolicy the service policy from which to parse the write protection status
     * @return {@code true} if write protection is enabled, {@code false} otherwise
     */
    protected Boolean parseWriteProtect(final ServicePolicy servicePolicy) {
        if (servicePolicy == null) {
            return null;
        }
        ServiceLivePolicy livePolicy = servicePolicy.getLivePolicy();
        return livePolicy == null ? null : livePolicy.isWriteProtect(request.getMethod());
    }

    /**
     * The {@code OutboundServiceMetadataParser} class is a concrete implementation of the
     * {@code ServiceMetadataParser} class, specifically designed to parse metadata for
     * outbound service requests.
     */
    public static class OutboundServiceMetadataParser extends ServiceMetadataParser {

        public OutboundServiceMetadataParser(ServiceRequest request, GovernancePolicy policy, InvocationContext context) {
            super(request, policy, context);
        }

        @Override
        public ServiceMetadata parse() {
            // add consumer name into carrier
            request.getOrCreateCarrier().setCargo(Constants.LABEL_SERVICE_CONSUMER, application.getName());
            return super.parse();
        }

        @Override
        protected String parseConsumer() {
            return application.getName();
        }

        @Override
        protected String parseServiceName() {
            return request.getService();
        }

        @Override
        protected String parseServiceGroup(final String serviceName) {
            String group = request.getGroup();
            if (group == null || group.isEmpty()) {
                group = config.getGroup(serviceName);
            }
            return group;
        }
    }

    /**
     * A parser implementation for extracting and building {@link ServiceMetadata} related to forwarding request.
     */
    public static class ForwardServiceMetadataParser implements ServiceParser {

        protected final ServiceConfig serviceConfig;

        public ForwardServiceMetadataParser(ServiceConfig serviceConfig) {
            this.serviceConfig = serviceConfig;
        }

        @Override
        public ServiceMetadata parse() {
            return ServiceMetadata.builder().serviceConfig(serviceConfig).build();
        }
    }

    /**
     * The {@code InboundServiceMetadataParser} class is a concrete implementation of the
     * {@code ServiceMetadataParser} class, specifically designed to parse metadata for
     * inbound service requests.
     */
    public static class InboundServiceMetadataParser extends ServiceMetadataParser {

        public InboundServiceMetadataParser(ServiceRequest request, GovernancePolicy policy, InvocationContext context) {
            super(request, policy, context);
        }

        @Override
        protected String parseConsumer() {
            Cargo cargo = request.getCargo(Constants.LABEL_SERVICE_CONSUMER);
            return cargo == null ? null : cargo.getFirstValue();
        }

        @Override
        protected String parseServiceName() {
            String result = application.getService().getName();
            return result != null && !result.isEmpty() ? result : request.getService();
        }

        @Override
        protected String parseServiceGroup(final String serviceName) {
            String result = application.getService().getGroup();
            return result != null && !result.isEmpty() ? result : request.getGroup();
        }

        @Override
        protected Service parseService(final String serviceName) {
            if (policy == null) {
                return null;
            }
            Service localService = policy.getLocalService();
            return localService != null && localService.getName().equals(serviceName) ? localService : policy.getService(serviceName);
        }
    }

    /**
     * The {@code HttpInboundServiceMetadataParser} class is a concrete implementation of the
     * {@code InboundServiceMetadataParser} class, specifically designed to parse metadata for
     * http inbound service requests.
     */
    public static class HttpInboundServiceMetadataParser extends InboundServiceMetadataParser {

        public HttpInboundServiceMetadataParser(ServiceRequest request, GovernancePolicy policy, InvocationContext context) {
            super(request, policy, context);
        }

        @Override
        protected Boolean parseWriteProtect(ServicePolicy servicePolicy) {
            Boolean result = super.parseWriteProtect(servicePolicy);
            return result != null ? result : isWriteMethod();
        }

        /**
         * Determines whether the current HTTP request method represents a write operation.
         * This is typically true for methods such as POST, PUT, DELETE, and PATCH, which modify resources.
         *
         * @return true if the request method is a write method, false otherwise
         */
        protected boolean isWriteMethod() {
            if (request == null) {
                return false;
            }
            HttpMethod httpMethod = ((HttpRequest) request).getHttpMethod();
            return httpMethod != null && httpMethod.isWrite();
        }
    }

    /**
     * The {@code GatewayInboundServiceMetadataParser} class is a concrete implementation of the
     * {@code HttpInboundServiceMetadataParser} class, specifically designed to parse metadata for
     * gateway inbound service requests.
     */
    public static class GatewayInboundServiceMetadataParser extends HttpInboundServiceMetadataParser {

        public GatewayInboundServiceMetadataParser(ServiceRequest request, GovernancePolicy policy, InvocationContext context) {
            super(request, policy, context);
        }

        @Override
        protected ServicePolicy parseServicePolicy(Service service, String serviceGroup, String path, String method) {
            ServicePolicy result = super.parseServicePolicy(service, serviceGroup, path, method);
            if (result == null) {
                return new ServicePolicy(new ServiceLivePolicy(UnitPolicy.NONE, CellPolicy.ANY));
            } else if (!liveEnabled) {
                return result;
            } else if (isValidLivePolicy(result.getLivePolicy())) {
                return result;
            }
            result = result.clone();
            result.setLivePolicy(new ServiceLivePolicy(UnitPolicy.NONE, CellPolicy.ANY));
            return result;
        }

        protected boolean isValidLivePolicy(final ServiceLivePolicy livePolicy) {
            return livePolicy != null
                    && (livePolicy.getUnitPolicy() == null || livePolicy.getUnitPolicy() == UnitPolicy.NONE)
                    && (livePolicy.getCellPolicy() == null || livePolicy.getCellPolicy() == CellPolicy.ANY);
        }
    }

    /**
     * The {@code GatewayOutboundServiceMetadataParser} class is a concrete implementation of the
     * {@code OutboundServiceMetadataParser} class, specifically designed to parse metadata for
     * gateway outbound service requests.
     */
    public static class GatewayOutboundServiceMetadataParser extends OutboundServiceMetadataParser {

        public GatewayOutboundServiceMetadataParser(ServiceRequest request, GovernancePolicy policy, InvocationContext context) {
            super(request, policy, context);
        }

        @Override
        public ServiceMetadata configure(ServiceMetadata metadata, UnitRule unitRule) {
            if (!liveEnabled) {
                return metadata;
            } else if (unitRule == null || unitRule.getLiveType() == LiveType.ONE_REGION_LIVE) {
                return metadata;
            }
            ServicePolicy servicePolicy = metadata.getServicePolicy();
            ServiceLivePolicy livePolicy = servicePolicy == null ? null : servicePolicy.getLivePolicy();
            if (isValidLivePolicy(livePolicy)) {
                return metadata;
            }
            livePolicy = livePolicy != null ? livePolicy.clone() : new ServiceLivePolicy();
            livePolicy.setUnitPolicy(UnitPolicy.PREFER_LOCAL_UNIT);
            livePolicy.setWriteProtect(metadata.isWriteProtect());
            servicePolicy = servicePolicy != null ? servicePolicy.clone() : new ServicePolicy();
            servicePolicy.setLivePolicy(livePolicy);
            return metadata.copyWith(servicePolicy);
        }

        protected boolean isValidLivePolicy(final ServiceLivePolicy livePolicy) {
            return livePolicy != null && livePolicy.getUnitPolicy() != null;
        }
    }
}
