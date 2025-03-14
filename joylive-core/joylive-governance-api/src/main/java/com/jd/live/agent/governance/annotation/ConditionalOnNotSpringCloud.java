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
package com.jd.live.agent.governance.annotation;

import com.jd.live.agent.core.extension.annotation.ConditionalComposite;
import com.jd.live.agent.core.extension.annotation.ConditionalOnMissingClass;

import java.lang.annotation.*;

/**
 * A conditional annotation that checks if a specific Spring Cloud class is missing from the classpath.
 * This annotation is used to conditionally enable or disable certain configurations or components
 * based on the absence of the specified Spring Cloud class.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ConditionalOnMissingClass(ConditionalOnNotSpringCloud.TYPE_LOAD_BALANCED)
@ConditionalComposite
public @interface ConditionalOnNotSpringCloud {

    String TYPE_LOAD_BALANCED = "org.springframework.cloud.client.loadbalancer.LoadBalanced";

}
