package com.jd.live.agent.plugin.router.rocketmq.v4.message;

import com.jd.live.agent.governance.request.AbstractMessage;
import org.apache.rocketmq.common.message.Message;

/**
 * A wrapper class for RocketMQ's {@link Message} object, extending the {@link AbstractMessage} class.
 */
public class RocketMQMessage extends AbstractMessage
        implements com.jd.live.agent.governance.request.Message.ProducerMessage {

    private final Message message;

    public RocketMQMessage(Message message) {
        super(message.getTopic(), message::getUserProperty);
        this.message = message;
    }

    @Override
    public void setTopic(String topic) {
        if (topic != null && !topic.isEmpty()) {
            this.topic = topic;
            message.setTopic(topic);
        }
    }

    @Override
    public void setHeader(String key, String value) {
        if (key != null && !key.isEmpty() && value != null && !value.isEmpty()) {
            message.putUserProperty(key, value);
        }
    }
}
