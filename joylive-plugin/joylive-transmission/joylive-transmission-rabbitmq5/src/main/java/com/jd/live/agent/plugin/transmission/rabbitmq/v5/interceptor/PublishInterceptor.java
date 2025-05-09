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
import com.jd.live.agent.core.instance.Location;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.context.RequestContext;
import com.jd.live.agent.governance.context.bag.Carrier;
import com.jd.live.agent.governance.context.bag.Propagation;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.request.HeaderWriter.ObjectMapWriter;
import com.jd.live.agent.governance.request.Message;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BasicProperties;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class PublishInterceptor extends InterceptorAdaptor {

    private final InvocationContext context;

    public PublishInterceptor(InvocationContext context) {
        this.context = context;
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        RequestContext.setAttribute(Carrier.ATTRIBUTE_MQ_PRODUCER, Boolean.TRUE);
        Object[] arguments = ctx.getArguments();
        int index = arguments.length - 2;
        BasicProperties properties = (BasicProperties) arguments[index];
        Map<String, Object> headers = writeTo(properties);
        if (!headers.isEmpty()) {
            arguments[index] = build(properties, headers);
        }
    }

    /**
     * Writes headers to the given {@link BasicProperties} object and returns the updated headers.
     *
     * @param properties the properties object to which headers will be written
     * @return the updated headers as a map
     */
    private Map<String, Object> writeTo(BasicProperties properties) {
        // the properties is unmodified
        Map<String, Object> headers = properties == null ? null : properties.getHeaders();
        Map<String, Object> newHeaders = new HashMap<>();
        String messageId = properties == null ? null : properties.getMessageId();
        if (messageId == null || messageId.isEmpty()) {
            messageId = headers == null ? null : (String) headers.get(Message.LABEL_MESSAGE_ID);
            if (messageId == null) {
                newHeaders.put(Message.LABEL_MESSAGE_ID, createMessageId());
            }
        }
        // write to the carrier
        Location location = context.isLiveEnabled() ? context.getLocation() : null;
        Propagation propagation = context.getPropagation();
        propagation.write(RequestContext.get(), location, new ObjectMapWriter(headers, newHeaders::put));
        if (!newHeaders.isEmpty() && headers != null) {
            newHeaders.putAll(headers);
        }
        return newHeaders;
    }

    /**
     * Builds a new {@link BasicProperties} object with the specified headers and message ID.
     *
     * @param properties the existing properties object, can be null
     * @param headers    the headers to be included in the new properties object
     * @return a new BasicProperties object with the specified headers and message ID
     */
    private BasicProperties build(BasicProperties properties, Map<String, Object> headers) {
        if (properties == null) {
            return new AMQP.BasicProperties().builder().headers(headers).build();
        }
        return properties instanceof AMQP.BasicProperties ?
                ((AMQP.BasicProperties) properties).builder().headers(headers).build() :
                new AMQP.BasicProperties(
                        properties.getContentType(), properties.getContentEncoding(), headers,
                        properties.getDeliveryMode(), properties.getPriority(), properties.getCorrelationId(),
                        properties.getReplyTo(), properties.getExpiration(), properties.getMessageId(),
                        properties.getTimestamp(), properties.getType(), properties.getUserId(),
                        properties.getAppId(), null);
    }

    /**
     * Creates a unique message ID.
     *
     * @return a unique message ID as a string
     */
    private String createMessageId() {
        long timestamp = System.nanoTime();
        int randomInt = ThreadLocalRandom.current().nextInt(1000000);
        return timestamp + "-" + randomInt;
    }

}
