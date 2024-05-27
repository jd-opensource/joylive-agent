package com.jd.live.agent.governance.invoke.metadata;

import com.jd.live.agent.governance.config.ServiceConfig;
import com.jd.live.agent.governance.policy.service.Service;
import com.jd.live.agent.governance.policy.service.ServicePolicy;
import com.jd.live.agent.governance.policy.service.live.ServiceLivePolicy;
import com.jd.live.agent.governance.policy.service.live.UnitPolicy;
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
    private boolean write;

    /**
     * The service metadata for this invocation.
     */
    private Service service;

    /**
     * The service policy applicable to this invocation.
     */
    private ServicePolicy servicePolicy;

    public UnitPolicy getUnitPolicy() {
        ServiceLivePolicy livePolicy = servicePolicy == null ? null : servicePolicy.getLivePolicy();
        UnitPolicy unitPolicy = livePolicy == null ? null : livePolicy.getUnitPolicy();
        return unitPolicy == null ? UnitPolicy.NONE : unitPolicy;
    }

    public ServiceLivePolicy getServiceLivePolicy() {
        return servicePolicy == null ? null : servicePolicy.getLivePolicy();
    }
}
