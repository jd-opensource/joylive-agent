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
package com.jd.live.agent.plugin.failover.rocketmq.v5.client;

import com.jd.live.agent.governance.db.mq.MQClientConfig;
import org.apache.rocketmq.client.ClientConfig;

/**
 * Base implementation of {@link MQClientConfig} for RocketMQ clients.
 */
public class RocketMQConfig implements MQClientConfig {

    protected static final String TYPE_ROCKETMQ = "rocketmq";

    protected static final String FIELD_RPC_HOOK = "rpcHook";
    protected static final String FIELD_REBALANCE_IMPL = "rebalanceImpl";
    protected static final String FIELD_HOOK_LIST = "consumeMessageHookList";
    protected static final String FIELD_CLIENT_FACTORY = "mQClientFactory";
    protected static final String FIELD_OFFSET_STORE = "offsetStore";
    protected static final String FIELD_TRACE_DISPATCHER = "traceDispatcher";
    // user object to fix class loader issue.
    protected Object target;

    public RocketMQConfig(Object target) {
        this.target = target;
    }

    @Override
    public String getServerAddress() {
        return ((ClientConfig) target).getNamesrvAddr();
    }

    @Override
    public void setServerAddress(String address) {
        ((ClientConfig) target).setNamesrvAddr(address);
    }

    @Override
    public String getType() {
        return TYPE_ROCKETMQ;
    }
}
