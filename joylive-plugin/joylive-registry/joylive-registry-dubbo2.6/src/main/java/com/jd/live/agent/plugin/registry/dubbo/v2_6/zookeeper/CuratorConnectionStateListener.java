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
package com.jd.live.agent.plugin.registry.dubbo.v2_6.zookeeper;

import com.alibaba.dubbo.remoting.zookeeper.StateListener;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;

import java.util.Set;

/**
 * Tracks ZooKeeper connection state changes and notifies listeners.
 */
public class CuratorConnectionStateListener implements ConnectionStateListener {

    protected static final Logger logger = LoggerFactory.getLogger(CuratorConnectionStateListener.class);

    private final long UNKNOWN_SESSION_ID = -1L;

    private final Set<StateListener> stateListeners;
    private final int timeout;
    private final int sessionExpireMs;

    private long lastSessionId;

    public CuratorConnectionStateListener(Set<StateListener> stateListeners, int timeout, int sessionExpireMs) {
        this.stateListeners = stateListeners;
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
            onLost();
        } else if (state == ConnectionState.SUSPENDED) {
            onSuspended(sessionId);
        } else if (state == ConnectionState.CONNECTED) {
            onConnected(sessionId);
        } else if (state == ConnectionState.RECONNECTED) {
            onReconnected(sessionId);
        }
    }

    /**
     * Handles successful reconnection where previous session is reused.
     *
     * @param sessionId The reused session ID
     */
    private void onReconnected(long sessionId) {
        if (lastSessionId == sessionId && sessionId != UNKNOWN_SESSION_ID) {
            logger.warn("Curator zookeeper connection recovered from connection lose, " +
                    "reuse the old session {}", Long.toHexString(sessionId));
            notify(ClientStateListener.RECONNECTED);
        } else {
            logger.warn("New session created after old session lost, " +
                    "old session {}, new session {}", Long.toHexString(lastSessionId), Long.toHexString(sessionId));
            lastSessionId = sessionId;
            notify(ClientStateListener.NEW_SESSION_CREATED);
        }
    }

    /**
     * Handles initial successful connection.
     *
     * @param sessionId The new session ID
     */
    private void onConnected(long sessionId) {
        lastSessionId = sessionId;
        logger.info("Curator zookeeper client instance initiated successfully, session id is {}", Long.toHexString(sessionId));
        notify(ClientStateListener.CONNECTED);
    }

    /**
     * Handles connection suspension (timeout before session expiration).
     *
     * @param sessionId The suspended session ID
     */
    private void onSuspended(long sessionId) {
        logger.warn("Curator zookeeper connection of session {} timed out. " +
                "connection timeout value is {}, session expire timeout value is {}", Long.toHexString(sessionId), timeout, sessionExpireMs);
        notify(ClientStateListener.SUSPENDED);
    }

    /**
     * Handles complete session expiration (requires new session creation).
     */
    private void onLost() {
        logger.warn("Curator zookeeper session {} expired.", Long.toHexString(lastSessionId));
        notify(ClientStateListener.SESSION_LOST);
    }

    /**
     * Notifies all registered state listeners about a state change.
     *
     * @param state the new state value to notify listeners about
     */
    private void notify(int state) {
        for (StateListener stateListener : stateListeners) {
            stateListener.stateChanged(state);
        }
    }

}
