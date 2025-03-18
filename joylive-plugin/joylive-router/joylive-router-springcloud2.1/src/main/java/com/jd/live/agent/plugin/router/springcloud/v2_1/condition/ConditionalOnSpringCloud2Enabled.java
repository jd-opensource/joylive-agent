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
package com.jd.live.agent.plugin.router.springcloud.v2_1.condition;

import com.jd.live.agent.core.extension.annotation.ConditionalComposite;
import com.jd.live.agent.core.extension.annotation.ConditionalOnMissingClass;
import com.jd.live.agent.governance.annotation.ConditionalOnSpringCloudEnabled;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ConditionalOnSpringCloudEnabled
@ConditionalOnMissingClass(ConditionalOnSpringCloud2Enabled.TYPE_SERVICE_INSTANCE_LIST_SUPPLIER)
@ConditionalComposite
public @interface ConditionalOnSpringCloud2Enabled {

    // spring cloud 2.2+
    String TYPE_SERVICE_INSTANCE_LIST_SUPPLIER = "org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier";
}
