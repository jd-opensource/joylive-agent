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
package com.jd.live.agent.plugin.protection.jedis.v4.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.config.RedisConfig;
import com.jd.live.agent.governance.interceptor.AbstractDbInterceptor;
import com.jd.live.agent.governance.policy.PolicySupplier;
import com.jd.live.agent.plugin.protection.jedis.v4.request.JedisRequest;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Connection;

/**
 * JedisConnectionInterceptor
 */
public class JedisConnectionInterceptor extends AbstractDbInterceptor {

    private final RedisConfig redisConfig;

    public JedisConnectionInterceptor(PolicySupplier policySupplier, GovernanceConfig governanceConfig) {
        super(policySupplier);
        this.redisConfig = governanceConfig.getRedisConfig();
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        Connection connection = (Connection) ctx.getTarget();
        CommandArguments args = ctx.getArgument(0);
        protect((MethodContext) ctx, new JedisRequest(connection, args.getCommand(), redisConfig::getAccessMode));
    }

}
