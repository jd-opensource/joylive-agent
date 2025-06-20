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
package com.jd.live.agent.governance.mq;

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.governance.db.DbConnection;
import com.jd.live.agent.governance.util.network.ClusterAddress;
import com.jd.live.agent.governance.util.network.ClusterRedirect;
import lombok.Getter;

/**
 * Abstract base class for MQ clients implementing {@link DbConnection}.
 * Manages client lifecycle (start/close/reconnect) with template methods.
 */
public abstract class AbstractMQClient implements MQClient {

    private static final Logger logger = LoggerFactory.getLogger(AbstractMQClient.class);

    // use object to fix classloader issue.
    protected final Object target;

    @Getter
    protected volatile ClusterRedirect address;

    protected volatile boolean closed = false;

    public AbstractMQClient(Object target, ClusterRedirect address) {
        this.target = target;
        this.address = address;
    }

    @Override
    public synchronized void close() {
        closed = true;
        doClose();
    }

    @Override
    public synchronized void start() throws Exception {
        doStart();
    }

    /**
     * Performs implementation-specific resource cleanup.
     * <p>Called during client shutdown.
     */
    protected abstract void doClose();

    /**
     * Performs implementation-specific initialization.
     *
     * @throws Exception if client fails to start
     */
    protected abstract void doStart() throws Exception;

    /**
     * Reconnects to a new cluster address and resets consumption offsets.
     *
     * @param newAddress the new cluster address to connect to
     */
    public synchronized void reconnect(ClusterAddress newAddress) {
        if (closed) {
            return;
        }
        String type = getType();
        String role = getRole().getName();
        String oldAddress = address.getNewAddress().getAddress();
        logger.info("Try redirecting the {} {} connection from {} to {}", type, role, oldAddress, newAddress);
        this.address = address.newAddress(newAddress);
        logger.info("Try closing the {} {} connection {}", type, role, oldAddress);
        doClose();
        logger.info("Success closing the {} {} connection {}", type, role, oldAddress);
        try {
            logger.info("Try connecting {} {} to {}", type, role, newAddress);
            setServerAddress(newAddress.getAddress());
            doStart();
            logger.info("Success connecting {} {} to {}", type, role, newAddress);
        } catch (Throwable e) {
            logger.error("Failed to reconnect {} {} to {}", type, role, newAddress, e);
        }
    }
}
