package com.jd.live.agent.plugin.router.rocketmq.v4.message;

import com.jd.live.agent.governance.request.AbstractMessage;
import org.apache.rocketmq.common.message.Message;

/**
 * A wrapper class for RocketMQ's {@link Message} object, extending the {@link AbstractMessage} class.
 */
public class RocketMQMessage extends AbstractMessage {

    public RocketMQMessage(Message message) {
        super(message.getTopic(), message::getUserProperty);
    }

}
