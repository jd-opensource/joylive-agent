package com.jd.live.agent.governance.invoke.metadata.parser;

import com.jd.live.agent.core.Constants;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.util.URI;
import com.jd.live.agent.core.util.http.HttpMethod;
import com.jd.live.agent.governance.config.ServiceConfig;
import com.jd.live.agent.governance.context.RequestContext;
import com.jd.live.agent.governance.context.bag.Cargo;
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
     * The service configuration containing parameters and settings for the service.
     */
    protected final ServiceConfig serviceConfig;

    /**
     * The application context which may provide additional service information.
     */
    protected final Application application;

    /**
     * The governance policy which defines the rules for service interaction.
     */
    protected final GovernancePolicy governancePolicy;

    /**
     * Constructs a new instance of {@code ServiceMetadataParser} with the specified parameters.
     *
     * @param request          the service request to be parsed
     * @param serviceConfig    the service configuration with parameters and settings
     * @param application      the application context providing additional service information
     * @param governancePolicy the governance policy defining rules for service interaction
     */
    public ServiceMetadataParser(ServiceRequest request,
                                 ServiceConfig serviceConfig,
                                 Application application,
                                 GovernancePolicy governancePolicy) {
        this.request = request;
        this.serviceConfig = serviceConfig;
        this.application = application;
        this.governancePolicy = governancePolicy;
    }

    @Override
    public ServiceMetadata parse() {
        String consumer = parseConsumer();
        String serviceName = parseServiceName();
        String serviceGroup = parseServiceGroup(serviceName);
        Service service = parseService(serviceName);
        String path = service == null ? parsePath() : service.getServiceType().normalize(parsePath());
        String method = parseMethod();
        ServicePolicy servicePolicy = parseServicePolicy(service, serviceGroup, path, method);
        boolean writeProtect = parseWriteProtect(servicePolicy);
        URI uri = URI.builder().host(serviceName).path(path).build();
        if (serviceGroup != null && !serviceGroup.isEmpty()) {
            uri = uri.parameters(KEY_SERVICE_GROUP, serviceGroup, KEY_SERVICE_METHOD, method);
        } else {
            uri = uri.parameters(KEY_SERVICE_METHOD, method);
        }

        return ServiceMetadata.builder().
                consumer(consumer).
                serviceConfig(serviceConfig).
                serviceName(serviceName).
                serviceGroup(serviceGroup).
                path(path).
                method(method).
                service(service).
                uri(uri).
                servicePolicy(servicePolicy).
                writeProtect(writeProtect).
                build();
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
        return governancePolicy == null ? null : governancePolicy.getService(serviceName);
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
    protected boolean parseWriteProtect(ServicePolicy servicePolicy) {
        ServiceLivePolicy livePolicy = servicePolicy == null ? null : servicePolicy.getLivePolicy();
        Boolean result = livePolicy != null ? livePolicy.isWriteProtect(request.getMethod()) : null;
        return result != null && result;
    }

    /**
     * The {@code OutboundServiceMetadataParser} class is a concrete implementation of the
     * {@code ServiceMetadataParser} class, specifically designed to parse metadata for
     * outbound service requests.
     */
    public static class OutboundServiceMetadataParser extends ServiceMetadataParser {

        public OutboundServiceMetadataParser(ServiceRequest request, ServiceConfig serviceConfig,
                                             Application application, GovernancePolicy governancePolicy) {
            super(request, serviceConfig, application, governancePolicy);
        }

        @Override
        protected String parseConsumer() {
            RequestContext.getOrCreate().setCargo(Constants.LABEL_SERVICE_CONSUMER, application.getName());
            return application.getName();
        }

        @Override
        protected String parseServiceName() {
            return request.getService();
        }

        @Override
        protected String parseServiceGroup(String serviceName) {
            String group = request.getGroup();
            if (group == null || group.isEmpty()) {
                Map<String, String> groups = serviceConfig.getServiceGroups();
                group = groups == null ? null : groups.get(serviceName);
            }
            return group;
        }
    }

    /**
     * The {@code InboundServiceMetadataParser} class is a concrete implementation of the
     * {@code ServiceMetadataParser} class, specifically designed to parse metadata for
     * inbound service requests.
     */
    public static class InboundServiceMetadataParser extends ServiceMetadataParser {

        public InboundServiceMetadataParser(ServiceRequest request, ServiceConfig serviceConfig,
                                            Application application, GovernancePolicy governancePolicy) {
            super(request, serviceConfig, application, governancePolicy);
        }

        @Override
        protected String parseConsumer() {
            Cargo cargo = RequestContext.getCargo(Constants.LABEL_SERVICE_CONSUMER);
            return cargo == null ? null : cargo.getFirstValue();
        }

        @Override
        protected String parseServiceName() {
            String result = application.getService().getName();
            result = result != null ? result : request.getService();
            return result;
        }

        @Override
        protected String parseServiceGroup(String serviceName) {
            String group = application.getService().getGroup();
            return group == null || group.isEmpty() ? request.getGroup() : group;
        }
    }

    /**
     * The {@code HttpInboundServiceMetadataParser} class is a concrete implementation of the
     * {@code InboundServiceMetadataParser} class, specifically designed to parse metadata for
     * http inbound service requests.
     */
    public static class HttpInboundServiceMetadataParser extends InboundServiceMetadataParser {

        public HttpInboundServiceMetadataParser(ServiceRequest request,
                                                ServiceConfig serviceConfig,
                                                Application application,
                                                GovernancePolicy governancePolicy) {
            super(request, serviceConfig, application, governancePolicy);
        }

        @Override
        protected boolean parseWriteProtect(ServicePolicy servicePolicy) {
            ServiceLivePolicy livePolicy = servicePolicy == null ? null : servicePolicy.getLivePolicy();
            Boolean result = livePolicy != null ? livePolicy.getWriteProtect() : null;
            if (result != null) {
                return result;
            } else {
                return isWriteMethod();
            }
        }

        /**
         * Determines whether the current HTTP request method represents a write operation.
         * This is typically true for methods such as POST, PUT, DELETE, and PATCH, which modify resources.
         *
         * @return true if the request method is a write method, false otherwise
         */
        protected boolean isWriteMethod() {
            HttpMethod httpMethod = request == null ? null : ((HttpRequest) request).getHttpMethod();
            return httpMethod != null && httpMethod.isWrite();
        }
    }

    /**
     * The {@code GatewayInboundServiceMetadataParser} class is a concrete implementation of the
     * {@code HttpInboundServiceMetadataParser} class, specifically designed to parse metadata for
     * gateway inbound service requests.
     */
    public static class GatewayInboundServiceMetadataParser extends HttpInboundServiceMetadataParser {

        public GatewayInboundServiceMetadataParser(ServiceRequest request,
                                                   ServiceConfig serviceConfig,
                                                   Application application,
                                                   GovernancePolicy governancePolicy) {
            super(request, serviceConfig, application, governancePolicy);
        }

        @Override
        protected ServicePolicy parseServicePolicy(Service service, String serviceGroup, String path, String method) {
            ServicePolicy result = super.parseServicePolicy(service, serviceGroup, path, method);
            result = result != null ? result.clone() : new ServicePolicy();
            ServiceLivePolicy livePolicy = new ServiceLivePolicy();
            livePolicy.setUnitPolicy(UnitPolicy.NONE);
            livePolicy.setCellPolicy(CellPolicy.ANY);
            livePolicy.setWriteProtect(isWriteMethod());
            result.setLivePolicy(livePolicy);
            return result;
        }
    }

    /**
     * The {@code GatewayOutboundServiceMetadataParser} class is a concrete implementation of the
     * {@code OutboundServiceMetadataParser} class, specifically designed to parse metadata for
     * gateway outbound service requests.
     */
    public static class GatewayOutboundServiceMetadataParser extends OutboundServiceMetadataParser {

        public GatewayOutboundServiceMetadataParser(ServiceRequest request,
                                                    ServiceConfig serviceConfig,
                                                    Application application,
                                                    GovernancePolicy governancePolicy) {
            super(request, serviceConfig, application, governancePolicy);
        }

        @Override
        public ServiceMetadata configure(ServiceMetadata metadata, UnitRule unitRule) {
            if (unitRule == null || unitRule.getLiveType() == LiveType.ONE_REGION_LIVE) {
                return metadata;
            }
            ServicePolicy policy = metadata.getServicePolicy();
            ServiceLivePolicy livePolicy = metadata.getServiceLivePolicy();
            UnitPolicy unitPolicy = metadata.getUnitPolicy();
            if (unitPolicy != UnitPolicy.NONE) {
                return metadata;
            }
            livePolicy = livePolicy != null ? livePolicy.clone() : new ServiceLivePolicy();
            livePolicy.setUnitPolicy(UnitPolicy.UNIT);
            livePolicy.setWriteProtect(metadata.isWriteProtect());
            ServicePolicy result = policy != null ? policy.clone() : new ServicePolicy();
            // TODO policyId;
            result.setLivePolicy(livePolicy);
            return ServiceMetadata.builder().
                    serviceConfig(metadata.getServiceConfig()).
                    serviceName(metadata.getServiceName()).
                    serviceGroup(metadata.getServiceGroup()).
                    path(metadata.getPath()).
                    method(metadata.getMethod()).
                    service(metadata.getService()).
                    servicePolicy(metadata.getServicePolicy()).
                    writeProtect(metadata.isWriteProtect()).build();
        }
    }
}
