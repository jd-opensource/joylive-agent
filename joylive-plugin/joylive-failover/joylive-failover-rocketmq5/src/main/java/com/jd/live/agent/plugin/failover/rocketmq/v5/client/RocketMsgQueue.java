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

import com.jd.live.agent.governance.db.mq.MsgQueue;
import lombok.Getter;
import org.apache.rocketmq.common.message.MessageQueue;

/**
 * RocketMQ implementation of {@link MsgQueue} interface.
 */
public class RocketMsgQueue implements MsgQueue {

    @Getter
    private final MessageQueue queue;

    public RocketMsgQueue(MessageQueue queue) {
        this.queue = queue;
    }

    @Override
    public String getTopic() {
        return queue.getTopic();
    }

    @Override
    public int getId() {
        return queue.getQueueId();
    }

    @Override
    public String toString() {
        return queue.getQueueId() + "@" + queue.getTopic() + "@" + queue.getBrokerName();
    }
}
