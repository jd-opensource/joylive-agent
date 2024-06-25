package com.jd.live.agent.plugin.router.kafka.v3.message;

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.governance.request.AbstractMessage;
import com.jd.live.agent.governance.request.Message;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * A concrete implementation of {@link AbstractMessage} for Kafka messages.
 */
public class KafkaProducerMessage extends AbstractMessage implements Message.ProducerMessage {

    private static final Logger logger = LoggerFactory.getLogger(KafkaProducerMessage.class);

    private final ProducerRecord<?, ?> record;

    public KafkaProducerMessage(ProducerRecord<?, ?> record) {
        super(record.topic(), key -> {
            Header header = record.headers().lastHeader(key);
            return header == null ? null : Arrays.toString(header.value());
        });
        this.record = record;
    }

    @Override
    public void setTopic(String topic) {
        if (topic != null && !topic.isEmpty()) {
            this.topic = topic;
            Updater.INSTANCE.setTopic(record, topic);
        }
    }

    @Override
    public void setHeader(String key, String value) {
        if (key != null && !key.isEmpty() && value != null && !value.isEmpty()) {
            record.headers().add(new RecordHeader(key, value.getBytes(StandardCharsets.UTF_8)));
        }
    }

    private static final class Updater {

        private static final Updater INSTANCE = new Updater();

        private Field field;

        Updater() {
            try {
                field = ProducerRecord.class.getDeclaredField("topic");
                field.setAccessible(true);
            } catch (Throwable e) {
                logger.error("Error occurs while accessing topic field of ProducerRecord.", e);
            }
        }

        public void setTopic(ProducerRecord<?, ?> record, String topic) {
            try {
                field.set(record, topic);
            } catch (Throwable e) {
                logger.error("Error occurs while accessing partition field of ProducerRecord.", e);
            }
        }

    }
}
