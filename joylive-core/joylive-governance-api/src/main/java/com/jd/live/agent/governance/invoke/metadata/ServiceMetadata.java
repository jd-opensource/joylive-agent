package com.jd.live.agent.governance.invoke.metadata;

import com.jd.live.agent.core.util.URI;
import com.jd.live.agent.governance.config.ServiceConfig;
import com.jd.live.agent.governance.policy.service.Service;
import com.jd.live.agent.governance.policy.service.ServicePolicy;
import com.jd.live.agent.governance.policy.service.live.CellPolicy;
import com.jd.live.agent.governance.policy.service.live.ServiceLivePolicy;
import com.jd.live.agent.governance.policy.service.live.UnitPolicy;
import com.jd.live.agent.governance.policy.service.loadbalance.LoadBalancePolicy;
import com.jd.live.agent.governance.policy.service.loadbalance.StickyType;
import lombok.Builder;
import lombok.Getter;

/**
 * The {@code ServiceMetadata} class encapsulates the metadata for a service request.
 */
@Getter
@Builder
public class ServiceMetadata {

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

    public UnitPolicy getUnitPolicy() {
        ServiceLivePolicy livePolicy = getServiceLivePolicy();
        UnitPolicy unitPolicy = livePolicy == null ? null : livePolicy.getUnitPolicy();
        return unitPolicy == null ? UnitPolicy.NONE : unitPolicy;
    }

    public CellPolicy getCellPolicy() {
        ServiceLivePolicy livePolicy = getServiceLivePolicy();
        CellPolicy cellPolicy = livePolicy == null ? null : livePolicy.getCellPolicy();
        return cellPolicy == null ? CellPolicy.ANY : cellPolicy;
    }

    public ServiceLivePolicy getServiceLivePolicy() {
        return servicePolicy == null ? null : servicePolicy.getLivePolicy();
    }

    public StickyType getStickyType() {
        LoadBalancePolicy loadBalancePolicy = servicePolicy == null ? null : servicePolicy.getLoadBalancePolicy();
        StickyType stickyType = loadBalancePolicy == null ? StickyType.NONE : loadBalancePolicy.getStickyType();
        return stickyType == null ? StickyType.NONE : stickyType;
    }
}
