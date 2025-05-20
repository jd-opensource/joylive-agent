package com.jd.live.agent.governance.invoke.metadata;

import com.jd.live.agent.core.util.URI;
import com.jd.live.agent.governance.config.ServiceConfig;
import com.jd.live.agent.governance.event.TrafficEvent.TrafficEventBuilder;
import com.jd.live.agent.governance.policy.PolicyId;
import com.jd.live.agent.governance.policy.service.Service;
import com.jd.live.agent.governance.policy.service.ServicePolicy;
import com.jd.live.agent.governance.policy.service.live.ServiceLivePolicy;
import com.jd.live.agent.governance.policy.service.live.UnitPolicy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * The {@code ServiceMetadata} class encapsulates the metadata for a service request.
 */
@Getter
@AllArgsConstructor
@Builder
public class ServiceMetadata implements Cloneable {

    /**
     * The service configuration for this invocation.
     */
    private ServiceConfig serviceConfig;

    /**
     * The name of the service being invoked.
     */
    private String serviceName;

    /**
     * The group of the service being invoked.
     */
    private String serviceGroup;

    /**
     * The path of the service being invoked.
     */
    private String path;

    /**
     * The HTTP method of the service request.
     */
    private String method;

    /**
     * Indicates if the service request is a write operation.
     */
    private boolean writeProtect;

    /**
     * The service metadata for this invocation.
     */
    private Service service;

    /**
     * The consumer for this service.
     */
    private String consumer;

    /**
     * The service policy applicable to this invocation.
     */
    private ServicePolicy servicePolicy;

    private URI uri;

    /**
     * Retrieves the default unit policy. If no specific policy is defined, {@link UnitPolicy#NONE} is returned.
     *
     * @return The unit policy, defaulting to {@link UnitPolicy#NONE} if not explicitly set.
     */
    public UnitPolicy getUnitPolicy() {
        return getUnitPolicy(UnitPolicy.NONE);
    }

    /**
     * Retrieves the unit policy, falling back to the provided default policy if no specific policy is defined.
     *
     * @param defaultPolicy The default unit policy to return if no specific policy is found.
     * @return The unit policy, or the provided default policy if no specific policy is set.
     */
    public UnitPolicy getUnitPolicy(UnitPolicy defaultPolicy) {
        ServiceLivePolicy livePolicy = getServiceLivePolicy();
        UnitPolicy unitPolicy = livePolicy == null ? null : livePolicy.getUnitPolicy();
        return unitPolicy == null ? defaultPolicy : unitPolicy;
    }

    /**
     * Retrieves the live policy associated with the service. If no live policy is defined, {@code null} is returned.
     *
     * @return The live policy for the service, or {@code null} if not set.
     */
    public ServiceLivePolicy getServiceLivePolicy() {
        return servicePolicy == null ? null : servicePolicy.getLivePolicy();
    }

    /**
     * Configures a live event builder with details from the current invocation context.
     *
     * @param builder The live event builder to configure.
     * @return The configured live event builder.
     */
    public TrafficEventBuilder configure(TrafficEventBuilder builder) {
        builder = builder.policyId(servicePolicy == null ? null : servicePolicy.getId());
        URI uri = servicePolicy == null ? null : servicePolicy.getUri();
        if (uri != null) {
            builder = builder.service(uri.getHost())
                    .group(uri.getParameter(PolicyId.KEY_SERVICE_GROUP))
                    .path(uri.getPath())
                    .method(uri.getParameter(PolicyId.KEY_SERVICE_METHOD));
        }
        return builder;
    }

    /**
     * Creates a copy of the current {@link ServiceMetadata} object with the provided {@link ServicePolicy}.
     *
     * @param servicePolicy The service policy to set in the copied object.
     * @return A new {@link ServiceMetadata} instance with the updated service policy.
     */
    public ServiceMetadata copyWith(ServicePolicy servicePolicy) {
        ServiceMetadata result = clone();
        result.servicePolicy = servicePolicy;
        return result;
    }

    public String getUniqueName() {
        return serviceGroup == null || serviceGroup.isEmpty() ? serviceName : serviceName + "@" + serviceGroup;
    }

    @Override
    public ServiceMetadata clone() {
        try {
            return (ServiceMetadata) super.clone();
        } catch (CloneNotSupportedException e) {
            return new ServiceMetadata(serviceConfig, serviceName, serviceGroup, path,
                    method, writeProtect, service, consumer, servicePolicy, uri);
        }
    }

}
