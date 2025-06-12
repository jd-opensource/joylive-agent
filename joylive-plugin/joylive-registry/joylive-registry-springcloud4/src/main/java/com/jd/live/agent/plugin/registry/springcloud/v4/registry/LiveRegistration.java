package com.jd.live.agent.plugin.registry.springcloud.v4.registry;
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
import org.springframework.cloud.client.serviceregistry.Registration;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class LiveRegistration implements Registration {

    private final Registration registration;

    private final Map<String, String> metadata;

    private final FrameworkVersion version;

    public LiveRegistration(Registration registration, Application application) {
        this.registration = registration;
        this.metadata = registration.getMetadata() == null ? new HashMap<>() : registration.getMetadata();
        String ver = Registration.class.getPackage().getImplementationVersion();
        ver = ver == null || ver.isEmpty() ? "4" : ver;
        this.version = new FrameworkVersion("spring-cloud", ver);
        application.labelRegistry(metadata::putIfAbsent, true);
        metadata.put(Constants.LABEL_FRAMEWORK, version.toString());
        if (registration.isSecure()) {
            metadata.put(Constants.LABEL_SECURE, String.valueOf(registration.isSecure()));
        }
    }

    @Override
    public String getServiceId() {
        return registration.getServiceId();
    }

    @Override
    public String getHost() {
        return registration.getHost();
    }

    @Override
    public int getPort() {
        return registration.getPort();
    }

    @Override
    public boolean isSecure() {
        return registration.isSecure();
    }

    @Override
    public URI getUri() {
        return registration.getUri();
    }

    @Override
    public Map<String, String> getMetadata() {
        return metadata;
    }

    @Override
    public String getInstanceId() {
        return registration.getInstanceId();
    }

    @Override
    public String getScheme() {
        return registration.getScheme();
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
                .scheme(getScheme())
                .host(getHost())
                .port(getPort())
                .metadata(getMetadata())
                .build();
    }
}
