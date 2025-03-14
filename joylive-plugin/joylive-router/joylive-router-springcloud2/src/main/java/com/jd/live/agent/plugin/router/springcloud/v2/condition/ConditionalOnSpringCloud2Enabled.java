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
package com.jd.live.agent.plugin.router.springcloud.v2.condition;

import com.jd.live.agent.core.extension.annotation.ConditionalComposite;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.ConditionalOnMissingClass;
import com.jd.live.agent.governance.annotation.ConditionalOnSpringEnabled;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ConditionalOnSpringEnabled
@ConditionalOnClass(ConditionalOnSpringCloud2Enabled.TYPE_SPRING_CLOUD_APPLICATION)
@ConditionalOnMissingClass(ConditionalOnSpringCloud2Enabled.TYPE_HINT_REQUEST_CONTEXT)
@ConditionalComposite
public @interface ConditionalOnSpringCloud2Enabled {

    String TYPE_HINT_REQUEST_CONTEXT = "org.springframework.cloud.client.loadbalancer.HintRequestContext";

    String TYPE_SPRING_CLOUD_APPLICATION = "org.springframework.cloud.client.SpringCloudApplication";
}
