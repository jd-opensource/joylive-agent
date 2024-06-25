package com.jd.live.agent.plugin.router.rocketmq.v5.message;

import com.jd.live.agent.governance.request.AbstractMessage;
import org.apache.rocketmq.common.message.Message;

/**
 * A concrete implementation of {@link AbstractMessage} for RocketMQ messages.
 */
public class RocketMQMessage extends AbstractMessage {

    public RocketMQMessage(Message message) {
        super(message.getTopic(), message::getUserProperty);
    }
}
