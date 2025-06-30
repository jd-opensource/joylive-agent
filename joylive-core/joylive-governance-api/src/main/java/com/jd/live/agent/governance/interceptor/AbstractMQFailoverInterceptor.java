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
package com.jd.live.agent.governance.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.LockContext;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.governance.db.DbCandidate;
import com.jd.live.agent.governance.db.DbFailover;
import com.jd.live.agent.governance.db.mq.MQClient;
import com.jd.live.agent.governance.db.mq.MQClientConfig;
import com.jd.live.agent.governance.event.DatabaseEvent;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.policy.AccessMode;

/**
 * AbstractMQFailoverInterceptor
 */
public abstract class AbstractMQFailoverInterceptor<C extends MQClient> extends AbstractDbConnectionInterceptor<C> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractMQFailoverInterceptor.class);

    private static final LockContext lock = new LockContext.DefaultLockContext();

    public AbstractMQFailoverInterceptor(InvocationContext context) {
        super(context);
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        // TransactionMQProducer inherit from DefaultMQProducer, will call start 2 times.
        if (ctx.tryLock(lock)) {
            MQClientConfig config = getClientConfig(ctx);
            DbCandidate candidate = getCandidate(config.getType(), config.getServerAddress());
            ctx.setAttribute(ATTR_OLD_ADDRESS, candidate);
            if (candidate.isRedirected()) {
                // Determine if a cluster failover was triggered
                config.setServerAddress(candidate.getNewAddress());
                logger.info("Try reconnecting to {} {}", config.getType(), candidate.getNewAddress());
            }
        }
    }

    @Override
    public void onSuccess(ExecutableContext ctx) {
        if (ctx.isLocked()) {
            DbCandidate oldCandidate = ctx.getAttribute(ATTR_OLD_ADDRESS);
            DbFailover failover = connectionSupervisor.failover(oldCandidate);
            C client = createClient(ctx, failover);
            connectionSupervisor.addConnection(client);
            // Avoid missing events caused by synchronous changes
            DbCandidate newCandidate = getCandidate(client.getType(), oldCandidate.getOldAddress());
            if (oldCandidate.isChanged(newCandidate)) {
                publisher.offer(new DatabaseEvent(this));
            }
        }
    }

    @Override
    public void onExit(ExecutableContext ctx) {
        ctx.unlock();
    }

    /**
     * Creates and returns the MQ client configuration for the given execution context.
     *
     * @param ctx the execution context containing environment and runtime information
     * @return fully configured MQ client settings (never null)
     */
    protected abstract MQClientConfig getClientConfig(ExecutableContext ctx);

    /**
     * Creates a client instance for given context and cluster configuration.
     *
     * @param ctx      execution context containing environment details
     * @param failover optional cluster redirection settings (nullable)
     * @return newly initialized client instance
     */
    protected abstract C createClient(ExecutableContext ctx, DbFailover failover);

    /**
     * Creates a database candidate with RW access and semicolon address resolver.
     *
     * @param type    database type (e.g., RocketMQ)
     * @param address server address(es)
     * @return pre-configured database candidate
     */
    protected DbCandidate getCandidate(String type, String address) {
        return connectionSupervisor.getCandidate(type, address, AccessMode.READ_WRITE, MULTI_ADDRESS_SEMICOLON_RESOLVER);
    }

}
