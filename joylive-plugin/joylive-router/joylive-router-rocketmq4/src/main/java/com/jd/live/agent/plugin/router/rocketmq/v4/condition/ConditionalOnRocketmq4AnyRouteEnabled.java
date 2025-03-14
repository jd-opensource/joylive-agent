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
package com.jd.live.agent.plugin.router.rocketmq.v4.condition;

import com.jd.live.agent.core.extension.annotation.ConditionalComposite;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.governance.annotation.ConditionalOnMqEnabled;
import com.jd.live.agent.governance.annotation.ConditionalOnOnlyRouteEnabled;
import com.jd.live.agent.governance.config.GovernanceConfig;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ConditionalOnOnlyRouteEnabled
@ConditionalOnMqEnabled
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_GOVERN_ROCKETMQ_ENABLED, matchIfMissing = true)
@ConditionalOnClass(ConditionalOnRocketmq4AnyRouteEnabled.TYPE_CLIENT_LOGGER)
@ConditionalComposite
public @interface ConditionalOnRocketmq4AnyRouteEnabled {

    String TYPE_CLIENT_LOGGER = "org.apache.rocketmq.client.log.ClientLogger";

}

