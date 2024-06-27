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

import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.core.util.tag.Label;
import com.jd.live.agent.governance.context.RequestContext;
import com.jd.live.agent.governance.context.bag.CargoRequire;
import com.jd.live.agent.governance.context.bag.CargoRequires;
import com.jd.live.agent.governance.request.Message;
import com.rabbitmq.client.BasicProperties;
import com.rabbitmq.client.Envelope;

import java.util.List;
import java.util.Map;

public class AbstractConsumerInterceptor extends InterceptorAdaptor {

    private final CargoRequire require;

    public AbstractConsumerInterceptor(List<CargoRequire> requires) {
        this.require = new CargoRequires(requires);
    }

    protected void restore(BasicProperties props, Envelope envelope) {
        if (props != null) {
            String messageId = props.getMessageId();
            Map<String, Object> headers = props.getHeaders();
            messageId = messageId == null ? props.getMessageId() : messageId;
            messageId = messageId == null && headers != null ? (String) headers.get(Message.LABEL_MESSAGE_ID) : messageId;
            messageId = messageId == null ? String.valueOf(envelope.getDeliveryTag()) : messageId;
            String id = "Rabbitmq5@" + envelope.getExchange() + "@" + messageId;
            RequestContext.restore(() -> id,
                    carrier -> carrier.addCargo(require, headers, Label::parseValue));
        }
    }

}
