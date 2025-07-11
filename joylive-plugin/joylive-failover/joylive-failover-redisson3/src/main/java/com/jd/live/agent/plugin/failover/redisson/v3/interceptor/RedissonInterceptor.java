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
package com.jd.live.agent.plugin.failover.redisson.v3.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.bootstrap.util.type.FieldAccessor;
import com.jd.live.agent.bootstrap.util.type.FieldAccessorFactory;
import com.jd.live.agent.governance.db.DbCandidate;
import com.jd.live.agent.governance.db.DbConnection;
import com.jd.live.agent.governance.db.DbFailover;
import com.jd.live.agent.governance.interceptor.AbstractDbConnectionInterceptor;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.policy.live.db.LiveDatabase;
import com.jd.live.agent.plugin.failover.redisson.v3.config.RedissonConfig;
import com.jd.live.agent.plugin.failover.redisson.v3.connection.RedissonConnection;
import org.redisson.Redisson;
import org.redisson.command.CommandAsyncExecutor;
import org.redisson.connection.ConnectionManager;
import org.redisson.liveobject.core.RedissonObjectBuilder;

import java.util.function.Function;

public class RedissonInterceptor extends AbstractDbConnectionInterceptor<DbConnection> {

    protected static final String TYPE_REDIS = "redis";

    private static final Logger logger = LoggerFactory.getLogger(RedissonInterceptor.class);

    public RedissonInterceptor(InvocationContext context) {
        super(context);
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        RedissonConfig config = new RedissonConfig(ctx.getArgument(0));
        ctx.setArgument(0, config);
        Function<LiveDatabase, String> addressResolver = config.isSingleConfig() ? PRIMARY_ADDRESS_RESOLVER : MULTI_ADDRESS_SEMICOLON_RESOLVER;
        DbCandidate candidate = connectionSupervisor.getCandidate(TYPE_REDIS, config.getAddress(), getAccessMode(null), addressResolver);
        if (candidate.isRedirected()) {
            config.setAddress(candidate.getNewNodes());
            logger.info("Try reconnecting to {} {}", TYPE_REDIS, candidate.getNewAddress());
        }
        ctx.setAttribute(ATTR_OLD_ADDRESS, candidate);
    }

    @Override
    public void onSuccess(ExecutableContext ctx) {
        DbCandidate candidate = ctx.getAttribute(ATTR_OLD_ADDRESS);
        if (candidate == null) {
            return;
        }
        Redisson redisson = (Redisson) ctx.getTarget();
        RedissonConfig config = (RedissonConfig) redisson.getConfig();
        RedissonConnection connection = new RedissonConnection(redisson, DbFailover.of(candidate), addr -> {
            // update address
            config.setAddress(addr.getNodes());
            // update connection manager
            ConnectionManager oldConnectionManager = Accessor.connectionManager.get(redisson, ConnectionManager.class);
            ConnectionManager newConnectionManager = ConnectionManager.create(new RedissonConfig(config));
            Accessor.connectionManager.set(redisson, newConnectionManager);
            // update command executor
            CommandAsyncExecutor oldExecutor = Accessor.commandExecutor.get(redisson, CommandAsyncExecutor.class);
            CommandAsyncExecutor newExecutor = newConnectionManager.createCommandExecutor(
                    config.isReferenceEnabled() ? new RedissonObjectBuilder(redisson) : null,
                    RedissonObjectBuilder.ReferenceType.DEFAULT);
            Accessor.commandExecutor.set(redisson, newExecutor);
            oldConnectionManager.shutdown();
        });
        checkFailover(createConnection(() -> connection), candidate.getAddressResolver());
    }

    private static class Accessor {

        private static final FieldAccessor connectionManager = FieldAccessorFactory.getAccessor(Redisson.class, "connectionManager");
        private static final FieldAccessor commandExecutor = FieldAccessorFactory.getAccessor(Redisson.class, "commandExecutor");

    }
}
