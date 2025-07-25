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
package com.jd.live.agent.plugin.registry.nacos.v2_4.condition;

import com.jd.live.agent.core.extension.annotation.ConditionalComposite;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.ConditionalOnMissingClass;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ConditionalOnClass(ConditionalOnNacos24Enabled.TYPE_SELECTOR_MANAGER)
@ConditionalOnMissingClass(ConditionalOnNacos24Enabled.TYPE_LOCK_SERVICE)
@ConditionalComposite
public @interface ConditionalOnNacos24Enabled {
    // 2.4+
    String TYPE_SELECTOR_MANAGER = "com.alibaba.nacos.client.selector.SelectorManager";
    // 3.0+
    String TYPE_LOCK_SERVICE = "com.alibaba.nacos.api.lock.LockService";

}
