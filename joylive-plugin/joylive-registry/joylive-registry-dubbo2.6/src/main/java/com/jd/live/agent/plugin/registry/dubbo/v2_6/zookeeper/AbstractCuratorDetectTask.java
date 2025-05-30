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

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.util.task.RetryExecution;
import com.jd.live.agent.core.util.time.Timer;
import com.jd.live.agent.governance.probe.HealthProbe;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A detect task that tests connectivity to ZooKeeper servers.
 * Requires consecutive successful checks to confirm recovery.
 */
public abstract class AbstractCuratorDetectTask implements RetryExecution {

    private static final Logger logger = LoggerFactory.getLogger(AbstractCuratorDetectTask.class);

    protected final HealthProbe probe;

    protected final int successThreshold;

    protected final int maxRetries;

    protected final boolean connected;

    protected final CuratorDetectTaskListener listener;

    protected final AtomicInteger successes = new AtomicInteger(0);

    protected final AtomicLong counter = new AtomicLong(0);

    public AbstractCuratorDetectTask(HealthProbe probe,
                                     int successThreshold,
                                     int maxRetries,
                                     boolean connected,
                                     CuratorDetectTaskListener listener) {
        this.probe = probe;
        this.successThreshold = successThreshold <= 0 ? 3 : successThreshold;
        this.maxRetries = maxRetries;
        this.connected = connected;
        this.listener = listener;
    }

    @Override
    public long getRetryInterval() {
        return connected ? Timer.getRetryInterval(1500, 3000) : 0;
    }

    /**
     * Tests connectivity to a ZooKeeper cluster and tracks consecutive successes.
     * Implements a quorum-based detection mechanism where majority servers must respond.
     */
    protected Connectivity detect(String address) {
        long count = counter.incrementAndGet();
        if (maxRetries > 0 && count > maxRetries) {
            return Connectivity.FAILURE_MAX_RETRIES;
        }
        if (count % 50 == 0) {
            logger.info("Test zookeeper connectivity {}, {} times.", address, count);
        }
        if (probe.test(address)) {
            if (successes.incrementAndGet() == successThreshold) {
                return Connectivity.SUCCESS_EXCEEDED;
            }
            return Connectivity.SUCCESS_BELOW;
        }
        successes.set(0);
        return Connectivity.FAILURE;
    }

    /**
     * Notifies the registered listener about a successful operation completion.
     */
    protected void onSuccess() {
        if (listener != null) {
            listener.onSuccess();
        }
    }

    /**
     * Notifies the registered listener about a failed operation.
     */
    protected void onFailure() {
        if (listener != null) {
            listener.onFailure();
        }
    }

    /**
     * Represents the possible outcomes of a ZooKeeper cluster connectivity test.
     */
    protected enum Connectivity {
        /**
         * Indicates stable connectivity has been established with sufficient consecutive successes.
         */
        SUCCESS_EXCEEDED,

        /**
         * Indicates the majority of servers are responsive but the consecutive success count
         * remains below the required threshold.
         */
        SUCCESS_BELOW,

        /**
         * Indicates the majority of servers are unresponsive after exhausting all retry attempts.
         */
        FAILURE_MAX_RETRIES,

        /**
         * Indicates the majority of servers are unresponsive, but retry attempts may still be available.
         */
        FAILURE
    }
}
