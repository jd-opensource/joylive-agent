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
package com.jd.live.agent.plugin.router.kafka.v2.condition;

import com.jd.live.agent.core.extension.annotation.ConditionalComposite;
import com.jd.live.agent.core.extension.annotation.ConditionalOnMissingClass;
import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.governance.annotation.ConditionalOnAnyRouteEnabled;
import com.jd.live.agent.governance.annotation.ConditionalOnMqEnabled;
import com.jd.live.agent.governance.config.GovernanceConfig;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ConditionalOnAnyRouteEnabled
@ConditionalOnMqEnabled
@ConditionalOnMissingClass(ConditionalOnKafka2AnyRouteEnabled.TYPE_ABORT_TRANSACTION_OPTIONS)
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_GOVERN_KAFKA_ENABLED, matchIfMissing = true)
@ConditionalComposite
public @interface ConditionalOnKafka2AnyRouteEnabled {

    // kafka client 3+
    String TYPE_ABORT_TRANSACTION_OPTIONS = "org.apache.kafka.clients.admin.AbortTransactionOptions";
}

