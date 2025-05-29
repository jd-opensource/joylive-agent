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
package com.jd.live.agent.plugin.registry.nacos.v1_4.condition;

import com.jd.live.agent.core.extension.annotation.ConditionalComposite;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.ConditionalOnMissingClass;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ConditionalOnClass(ConditionalOnNacos14Enabled.TYPE_CREDENTIALS)
@ConditionalOnMissingClass(ConditionalOnNacos14Enabled.TYPE_BETA)
@ConditionalComposite
public @interface ConditionalOnNacos14Enabled {

    // 1.4/2.0
    String TYPE_CREDENTIALS = "com.alibaba.nacos.client.identify.Credentials";

    // 2.0
    String TYPE_BETA = "com.alibaba.nacos.common.Beta";

}
