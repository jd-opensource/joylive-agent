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
package com.jd.live.agent.demo.rocketmq.consumer;

import com.jd.live.agent.demo.rocketmq.service.ConsumerService;
import com.jd.live.agent.demo.util.EchoResponse;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQReplyListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@RocketMQMessageListener(topic = "${rocketmq.topic}", consumerGroup = "${rocketmq.consumer.group}")
public class RocketmqConsumer implements RocketMQReplyListener<MessageExt, String> {

    private static final Logger logger = LoggerFactory.getLogger(RocketmqConsumer.class);

    private final ConsumerService consumerService;

    public RocketmqConsumer(ConsumerService consumerService) {
        this.consumerService = consumerService;
    }

    @Override
    public String onMessage(MessageExt message) {
        Map<String, String> properties = message.getProperties();
        String result;
        try {
            String msg = consumerService.echo(new String(message.getBody(), StandardCharsets.UTF_8));
            result = new EchoResponse("spring-rocketmq-consumer", "properties", properties::get, msg).toString();
        } catch (Throwable e) {
            result = new EchoResponse("spring-rocketmq-consumer", "properties", properties::get, e.getMessage()).toString();
        }
        logger.info(result);
        return result;
    }
}
