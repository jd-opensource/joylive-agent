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
package com.jd.live.agent.plugin.protection.jedis.v3.condition;

import com.jd.live.agent.core.extension.annotation.ConditionalComposite;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.ConditionalOnMissingClass;
import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.governance.annotation.ConditionalOnProtectDBEnabled;
import com.jd.live.agent.governance.config.GovernanceConfig;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ConditionalOnProtectDBEnabled
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_FAILOVER_REDIS_ENABLED)
@ConditionalOnClass(ConditionalOnProtectJedis3Enabled.TYPE_HOST_AND_PORT)
@ConditionalOnMissingClass(ConditionalOnProtectJedis3Enabled.TYPE_GEO_UNIT)
@ConditionalComposite
public @interface ConditionalOnProtectJedis3Enabled {

    String TYPE_HOST_AND_PORT = "redis.clients.jedis.HostAndPort";

    String TYPE_GEO_UNIT = "redis.clients.jedis.args.GeoUnit";

}
