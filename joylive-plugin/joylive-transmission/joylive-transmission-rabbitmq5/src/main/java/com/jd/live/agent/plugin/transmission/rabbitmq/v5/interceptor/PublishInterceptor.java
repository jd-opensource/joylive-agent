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
package com.jd.live.agent.plugin.transmission.rabbitmq.v5.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.context.RequestContext;
import com.jd.live.agent.governance.context.bag.Carrier;
import com.jd.live.agent.governance.context.bag.Propagation;
import com.jd.live.agent.governance.request.Message;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BasicProperties;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static com.jd.live.agent.governance.request.header.HeaderParser.ObjectHeaderParser.writer;

public class PublishInterceptor extends InterceptorAdaptor {

    private final Propagation propagation;

    public PublishInterceptor(Propagation propagation) {
        this.propagation = propagation;
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        RequestContext.setAttribute(Carrier.ATTRIBUTE_MQ_PRODUCER, Boolean.TRUE);
        Object[] arguments = ctx.getArguments();
        BasicProperties properties = (BasicProperties) arguments[arguments.length - 2];
        if (properties == null) {
            AMQP.BasicProperties p = new AMQP.BasicProperties().builder().headers(new HashMap<>()).build();
            arguments[arguments.length - 2] = p;
            properties = p;
        }
        Map<String, Object> headers = properties.getHeaders();
        String messageId = properties.getMessageId();
        if (headers == null) {
            headers = new HashMap<>();
            AMQP.BasicProperties p = properties instanceof AMQP.BasicProperties ?
                    ((AMQP.BasicProperties) properties).builder().headers(headers).build() :
                    new AMQP.BasicProperties(
                            properties.getContentType(), properties.getContentEncoding(), headers,
                            properties.getDeliveryMode(), properties.getPriority(), properties.getCorrelationId(),
                            properties.getReplyTo(), properties.getExpiration(), messageId,
                            properties.getTimestamp(), properties.getType(), properties.getUserId(),
                            properties.getAppId(), null);
            arguments[arguments.length - 2] = p;
        }
        if (messageId == null || messageId.isEmpty()) {
            long timestamp = System.nanoTime();
            int randomInt = ThreadLocalRandom.current().nextInt(1000000);
            messageId = timestamp + "-" + randomInt;
            headers.put(Message.LABEL_MESSAGE_ID, messageId);
        }
        propagation.write(RequestContext.get(), writer(headers, headers::put));
    }

}
