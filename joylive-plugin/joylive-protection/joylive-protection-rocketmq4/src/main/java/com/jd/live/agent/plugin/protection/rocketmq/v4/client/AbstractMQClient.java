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
package com.jd.live.agent.plugin.protection.rocketmq.v4.client;

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.governance.db.DbConnection;
import com.jd.live.agent.governance.util.network.ClusterAddress;
import com.jd.live.agent.governance.util.network.ClusterRedirect;
import lombok.Getter;
import org.apache.rocketmq.client.ClientConfig;
import org.apache.rocketmq.client.exception.MQClientException;

/**
 * Abstract base class for MQ clients implementing {@link DbConnection}.
 * Manages client lifecycle (start/close/reconnect) with template methods.
 *
 * @param <T> Type of client configuration, must extend {@link ClientConfig}
 */
public abstract class AbstractMQClient<T extends ClientConfig> implements DbConnection {

    private static final Logger logger = LoggerFactory.getLogger(AbstractMQClient.class);

    protected static final String FIELD_RPC_HOOK = "rpcHook";
    protected static final String FIELD_TRACE_DISPATCHER = "traceDispatcher";
    public static final String TYPE_ROCKETMQ = "rocketmq";

    protected final T target;

    @Getter
    protected volatile ClusterRedirect address;

    protected volatile boolean closed = false;

    public AbstractMQClient(T target, ClusterRedirect address) {
        this.target = target;
        this.address = address;
    }

    @Override
    public synchronized void close() {
        closed = true;
        doClose();
    }

    protected abstract String getType();

    /**
     * Implementation-specific resource cleanup.
     */
    protected abstract void doClose();

    /**
     * Implementation-specific client initialization.
     *
     * @throws MQClientException if startup fails
     */
    protected abstract void doStart() throws MQClientException;

    /**
     * Reconnects to a new cluster address and resets consumption offsets.
     *
     * @param newAddress the new cluster address to connect to
     */
    public synchronized void reconnect(ClusterAddress newAddress) {
        if (closed) {
            return;
        }
        logger.info("Try redirecting the rocketmq {} connection from {} to {}", getType(), address.getOldAddress(), newAddress);
        this.address = address.newAddress(newAddress);
        logger.info("Try closing the rocketmq {} connection {}", getType(), target.getNamesrvAddr());
        doClose();
        logger.info("Success closing the rocketmq {} connection {}", getType(), target.getNamesrvAddr());
        try {
            logger.info("Try connecting rocketmq {} to {}", getType(), newAddress);
            target.setNamesrvAddr(newAddress.getAddress());
            doStart();
            logger.info("Success connecting rocketmq {} to {}", getType(), newAddress);
        } catch (Throwable e) {
            logger.error("Failed to reconnect rocketmq {} to {}", getType(), newAddress, e);
        }
    }
}
