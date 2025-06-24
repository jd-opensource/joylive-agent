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
package com.jd.live.agent.plugin.failover.jedis.v5.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.governance.event.DatabaseEvent;
import com.jd.live.agent.governance.interceptor.AbstractDbConnectionInterceptor;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.policy.AccessMode;
import com.jd.live.agent.governance.util.network.ClusterAddress;
import com.jd.live.agent.governance.util.network.ClusterRedirect;
import com.jd.live.agent.plugin.failover.jedis.v5.connection.JedisConnection;
import redis.clients.jedis.JedisClientConfig;

/**
 * AbstractJedisInterceptor
 */
public abstract class AbstractJedisInterceptor extends AbstractDbConnectionInterceptor<JedisConnection> {

    protected static final String TYPE_REDIS = "redis";

    public AbstractJedisInterceptor(InvocationContext context) {
        super(context);
    }

    @Override
    public void onSuccess(ExecutableContext ctx) {
        JedisConnection connection = createConnection(() -> createConnection(ctx));
        if (connection == null) {
            return;
        }
        ClusterRedirect address = connection.getAddress();
        ClusterRedirect.redirect(address, address.isRedirected() ? consumer : null);
        // Avoid missing events caused by synchronous changes
        DbCandidate newCandidate = getCandidate(address, PRIMARY_ADDRESS_RESOLVER);
        if (isChanged(address.getNewAddress(), newCandidate)) {
            publisher.offer(new DatabaseEvent(this));
        }
    }

    @Override
    protected void redirectTo(JedisConnection connection, ClusterAddress address) {
        ClusterRedirect.redirect(connection.redirect(address), consumer);
    }

    /**
     * Gets the access mode from client configuration.
     *
     * @param clientConfig the Jedis client configuration
     * @return the determined access mode
     */
    protected AccessMode getAccessMode(JedisClientConfig clientConfig) {
        return getAccessMode(clientConfig.getClientName(), null, null);
    }

    /**
     * Creates a new Jedis connection for the given execution context.
     *
     * @param ctx the execution context
     * @return a new Jedis connection instance
     */
    protected abstract JedisConnection createConnection(ExecutableContext ctx);

}
