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
package com.jd.live.agent.plugin.registry.nacos.v2_3.condition;

import com.jd.live.agent.core.extension.annotation.ConditionalComposite;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.ConditionalOnMissingClass;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ConditionalOnClass(ConditionalOnNacos23Enabled.TYPE_SOURCE_TYPE)
@ConditionalOnMissingClass(ConditionalOnNacos23Enabled.TYPE_SELECTOR_MANAGER)
@ConditionalComposite
public @interface ConditionalOnNacos23Enabled {

    // Compatible with 2.1+
    String TYPE_SOURCE_TYPE = "com.alibaba.nacos.client.env.SourceType";

    // 2.3+
    String TYPE_SELECTOR_MANAGER = "com.alibaba.nacos.client.selector.SelectorManager";

}
