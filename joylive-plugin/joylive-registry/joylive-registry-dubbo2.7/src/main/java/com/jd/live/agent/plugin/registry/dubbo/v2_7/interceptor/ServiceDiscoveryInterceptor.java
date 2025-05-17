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
package com.jd.live.agent.plugin.registry.dubbo.v2_7.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.parser.ObjectParser;
import com.jd.live.agent.core.parser.TypeReference;
import com.jd.live.agent.core.util.option.MapOption;
import com.jd.live.agent.governance.interceptor.AbstractRegistryInterceptor;
import com.jd.live.agent.governance.registry.Registry;
import com.jd.live.agent.governance.registry.ServiceInstance;
import com.jd.live.agent.governance.util.FrameworkVersion;

import java.io.StringReader;
import java.util.Map;

/**
 * RegistryInterceptor
 */
public class ServiceDiscoveryInterceptor extends AbstractRegistryInterceptor {

    private final ObjectParser jsonParser;

    public ServiceDiscoveryInterceptor(Application application, Registry registry, ObjectParser jsonParser) {
        super(application, registry);
        this.jsonParser = jsonParser;
    }

    @Override
    protected ServiceInstance getInstance(MethodContext ctx) {
        // TODO may called multiple times
        // multiple service discovery instances.
        // MultipleRegistryServiceDiscovery
        org.apache.dubbo.registry.client.ServiceInstance instance = ctx.getArgument(0);
        Map<String, String> metadata = instance.getMetadata();
        application.labelRegistry(metadata::putIfAbsent);
        MapOption option = new MapOption(metadata);
        String params = option.getString("dubbo.metadata-service.url-params");
        Map<String, Map<String, String>> urlParams = params == null ? null : jsonParser.read(new StringReader(params), new TypeReference<Map<String, Map<String, String>>>() {
        });
        Map<String, String> dubboParams = urlParams == null ? null : urlParams.get("dubbo");
        MapOption urlOption = new MapOption(dubboParams);
        return ServiceInstance.builder()
                .id(instance.getId())
                .interfaceMode(false)
                .framework(new FrameworkVersion("dubbo", urlOption.getString("release", "2.7")))
                .service(instance.getServiceName())
                .scheme(urlOption.getString("protocol", "dubbo"))
                .group(option.getString("group"))
                .host(instance.getHost())
                .port(instance.getPort())
                .weight(option.getInteger("weight", 100))
                .metadata(metadata)
                .build();
    }
}
