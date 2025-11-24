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
package com.jd.live.agent.plugin.application.springboot.register;

import org.springframework.context.ConfigurableApplicationContext;

/**
 * Interface for registering beans in a Spring application context.
 * Implementations can register custom beans during application startup.
 */
public interface BeanRegister {

    /**
     * Registers beans in the provided application context.
     *
     * @param ctx The configurable application context where beans will be registered
     */
    void register(ConfigurableApplicationContext ctx);
}
