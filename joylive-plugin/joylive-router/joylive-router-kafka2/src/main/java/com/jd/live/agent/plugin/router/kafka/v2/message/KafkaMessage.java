package com.jd.live.agent.plugin.router.kafka.v2.message;

import com.jd.live.agent.governance.request.AbstractMessage;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;

import java.util.Arrays;

/**
 * A concrete implementation of {@link AbstractMessage} for Kafka messages.
 *
 * @since 1.8.0
 */
public class KafkaMessage extends AbstractMessage {

    public KafkaMessage(ConsumerRecord<?, ?> record) {
        super(record.topic(), key -> {
            Header header = record.headers().lastHeader(key);
            return header == null ? null : Arrays.toString(header.value());
        });
    }
}
