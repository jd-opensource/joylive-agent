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
package com.jd.live.agent.plugin.failover.rocketmq.v4.client;

import com.jd.live.agent.governance.mq.AbstractMQConsumer;
import com.jd.live.agent.governance.util.network.ClusterRedirect;
import org.apache.rocketmq.client.ClientConfig;

import static com.jd.live.agent.plugin.failover.rocketmq.v4.client.RocketMQConfig.TYPE_ROCKETMQ;

/**
 * Abstract base class for MQ consumer clients.
 * Handles message queue seeking operations with timestamp-based offsets.
 */
public abstract class AbstractRocketMQConsumer extends AbstractMQConsumer {

    public AbstractRocketMQConsumer(Object target, ClusterRedirect address) {
        super(target, address);
    }

    @Override
    public String getType() {
        return TYPE_ROCKETMQ;
    }

    @Override
    public String getServerAddress() {
        return ((ClientConfig) target).getNamesrvAddr();
    }

    @Override
    public void setServerAddress(String address) {
        ((ClientConfig) target).setNamesrvAddr(address);
    }

}
