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
package com.jd.live.agent.plugin.registry.springgateway.v2.condition;

import com.jd.live.agent.core.extension.annotation.ConditionalComposite;
import com.jd.live.agent.core.extension.annotation.ConditionalOnMissingClass;
import com.jd.live.agent.governance.annotation.ConditionalOnReactive;
import com.jd.live.agent.governance.annotation.ConditionalOnSpringGatewayEnabled;

import java.lang.annotation.*;

/**
 * An annotation used to mark a type as requiring specific conditions related to Spring Gateway to be met.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ConditionalOnSpringGatewayEnabled
@ConditionalOnMissingClass(ConditionalOnSpringGateway2Enabled.TYPE_STICKY_SESSION_SUPPLIER)
@ConditionalOnReactive
@ConditionalComposite
public @interface ConditionalOnSpringGateway2Enabled {

    // spring gateway 3/4
    String TYPE_STICKY_SESSION_SUPPLIER = "org.springframework.cloud.loadbalancer.core.RequestBasedStickySessionServiceInstanceListSupplier";
}
