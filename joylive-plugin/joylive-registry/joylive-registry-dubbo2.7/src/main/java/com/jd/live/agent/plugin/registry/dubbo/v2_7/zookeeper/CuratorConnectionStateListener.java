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
package com.jd.live.agent.plugin.registry.dubbo.v2_7.zookeeper;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.zookeeper.StateListener;

import java.util.function.Consumer;

/**
 * Tracks ZooKeeper connection state changes and notifies listeners.
 */
public class CuratorConnectionStateListener implements ConnectionStateListener {

    protected static final Logger logger = LoggerFactory.getLogger(CuratorConnectionStateListener.class);

    private final long UNKNOWN_SESSION_ID = -1L;

    private final Consumer<Integer> stateListener;
    private final int timeout;
    private final int sessionExpireMs;

    private long lastSessionId;

    public CuratorConnectionStateListener(Consumer<Integer> stateListener, int timeout, int sessionExpireMs) {
        this.stateListener = stateListener;
        this.timeout = timeout;
        this.sessionExpireMs = sessionExpireMs;
    }

    @Override
    public void stateChanged(CuratorFramework client, ConnectionState state) {
        long sessionId = UNKNOWN_SESSION_ID;
        try {
            sessionId = client.getZookeeperClient().getZooKeeper().getSessionId();
        } catch (Exception e) {
            logger.warn("Curator client state changed, but failed to get the related zk session instance.");
        }

        if (state == ConnectionState.LOST) {
            logger.warn("Curator zookeeper session " + Long.toHexString(lastSessionId) + " expired.");
            stateListener.accept(StateListener.SESSION_LOST);
        } else if (state == ConnectionState.SUSPENDED) {
            logger.warn("Curator zookeeper connection of session " + Long.toHexString(sessionId) + " timed out. " +
                    "connection timeout value is " + timeout + ", session expire timeout value is " + sessionExpireMs);
            stateListener.accept(StateListener.SUSPENDED);
        } else if (state == ConnectionState.CONNECTED) {
            lastSessionId = sessionId;
            logger.info("Curator zookeeper client instance initiated successfully, session id is " + Long.toHexString(sessionId));
            stateListener.accept(StateListener.CONNECTED);
        } else if (state == ConnectionState.RECONNECTED) {
            if (lastSessionId == sessionId && sessionId != UNKNOWN_SESSION_ID) {
                logger.warn("Curator zookeeper connection recovered from connection lose, " +
                        "reuse the old session " + Long.toHexString(sessionId));
                stateListener.accept(StateListener.RECONNECTED);
            } else {
                logger.warn("New session created after old session lost, " +
                        "old session " + Long.toHexString(lastSessionId) + ", new session " + Long.toHexString(sessionId));
                lastSessionId = sessionId;
                stateListener.accept(StateListener.NEW_SESSION_CREATED);
            }
        }
    }

}
