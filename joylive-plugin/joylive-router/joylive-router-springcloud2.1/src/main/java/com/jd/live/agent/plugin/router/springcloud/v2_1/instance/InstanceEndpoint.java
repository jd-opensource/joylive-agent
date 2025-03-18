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
package com.jd.live.agent.plugin.router.springcloud.v2_1.instance;

import com.jd.live.agent.governance.registry.ServiceEndpoint;
import org.springframework.cloud.client.ServiceInstance;

import java.net.URI;

/**
 * An interface that combines the functionality of both {@link ServiceEndpoint} and {@link ServiceInstance}.
 * This interface provides default implementations for methods inherited from both parent interfaces.
 */
public interface InstanceEndpoint extends ServiceEndpoint, ServiceInstance {

    @Override
    default URI getUri() {
        return ServiceEndpoint.super.getUri();
    }

    @Override
    default boolean isSecure() {
        return ServiceEndpoint.super.isSecure();
    }
}
