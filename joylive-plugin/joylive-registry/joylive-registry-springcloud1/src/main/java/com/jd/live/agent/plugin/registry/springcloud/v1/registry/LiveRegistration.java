package com.jd.live.agent.plugin.registry.springcloud.v1.registry;
/*
 * Copyright © ${year} ${owner} (${email})
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.jd.live.agent.core.Constants;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.governance.registry.ServiceInstance;
import com.jd.live.agent.governance.util.FrameworkVersion;
import com.netflix.appinfo.InstanceInfo;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.cloud.netflix.eureka.serviceregistry.EurekaRegistration;

import java.util.HashMap;
import java.util.Map;

public class LiveRegistration implements Registration {

    private final Registration registration;

    private final String host;

    private final int port;

    private final Map<String, String> metadata;

    private final FrameworkVersion version;

    public LiveRegistration(Registration registration, Application application) {
        this.registration = registration;
        // EurekaRegistration is not implement ServiceInstance
        // Eureka is handled by joylive-registry-eureka module
        org.springframework.cloud.client.ServiceInstance instance = getInstance(registration);
        this.host = instance == null ? null : instance.getHost();
        this.port = instance == null ? -1 : instance.getPort();
        this.metadata = instance == null ? new HashMap<>() : instance.getMetadata();
        this.version = new FrameworkVersion("spring-cloud", Registration.class, "1");
        application.labelRegistry(metadata::putIfAbsent, true);
        metadata.put(Constants.LABEL_FRAMEWORK, version.toString());
        if (instance != null && instance.isSecure()) {
            metadata.put(Constants.LABEL_SECURE, String.valueOf(Boolean.TRUE));
        }
    }

    @Override
    public String getServiceId() {
        return registration.getServiceId();
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public String getMetadata(String key) {
        return metadata.get(key);
    }

    public FrameworkVersion getVersion() {
        return version;
    }

    public ServiceInstance toInstance() {
        return ServiceInstance.builder()
                .interfaceMode(false)
                .framework(version)
                .service(getServiceId())
                .group(getMetadata(Constants.LABEL_SERVICE_GROUP))
                .host(host)
                .port(port)
                .metadata(metadata)
                .build();
    }

    private static org.springframework.cloud.client.ServiceInstance getInstance(Registration registration) {
        if (registration instanceof org.springframework.cloud.client.ServiceInstance) {
            return (org.springframework.cloud.client.ServiceInstance) registration;
        } else if (registration.getClass().getName().equals("org.springframework.cloud.netflix.eureka.serviceregistry.EurekaRegistration")) {
            EurekaRegistration eurekaRegistration = (EurekaRegistration) registration;
            InstanceInfo info = eurekaRegistration.getApplicationInfoManager().getInfo();
            boolean secure = info.isPortEnabled(InstanceInfo.PortType.SECURE) && !info.isPortEnabled(InstanceInfo.PortType.UNSECURE);
            int port = secure ? info.getSecurePort() : info.getPort();
            return new DefaultServiceInstance(registration.getServiceId(), info.getHostName(), port, secure, info.getMetadata());
        } else {
            return null;
        }
    }

}
