package com.jd.live.agent.governance.invoke.metadata;

import com.jd.live.agent.core.util.URI;
import com.jd.live.agent.governance.config.ServiceConfig;
import com.jd.live.agent.governance.policy.service.Service;
import com.jd.live.agent.governance.policy.service.ServicePolicy;
import com.jd.live.agent.governance.policy.service.live.ServiceLivePolicy;
import com.jd.live.agent.governance.policy.service.live.UnitPolicy;
import com.jd.live.agent.governance.policy.service.loadbalance.LoadBalancePolicy;
import com.jd.live.agent.governance.policy.service.loadbalance.StickyType;
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
     * Retrieves the sticky type for load balancing. If no sticky type is defined, {@link StickyType#NONE} is returned.
     *
     * @return The sticky type for load balancing, defaulting to {@link StickyType#NONE} if not explicitly set.
     */
    public StickyType getStickyType() {
        LoadBalancePolicy loadBalancePolicy = servicePolicy == null ? null : servicePolicy.getLoadBalancePolicy();
        StickyType stickyType = loadBalancePolicy == null ? StickyType.NONE : loadBalancePolicy.getStickyType();
        return stickyType == null ? StickyType.NONE : stickyType;
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
