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
package com.jd.live.agent.plugin.transmission.rocketmq.v5.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.core.instance.Location;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.context.RequestContext;
import com.jd.live.agent.governance.context.bag.Carrier;
import com.jd.live.agent.governance.context.bag.Propagation;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.request.HeaderWriter.StringMapWriter;
import org.apache.rocketmq.common.message.Message;

import java.util.Collection;

public class MQProducerInterceptor extends InterceptorAdaptor {

    private final InvocationContext context;

    public MQProducerInterceptor(InvocationContext context) {
        this.context = context;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onEnter(ExecutableContext ctx) {
        Object argument = ctx.getArgument(0);
        RequestContext.setAttribute(Carrier.ATTRIBUTE_MQ_PRODUCER, Boolean.TRUE);
        Location location = context.isLiveEnabled() ? context.getLocation() : null;
        Propagation propagation = context.getPropagation();
        if (argument instanceof Message) {
            Message message = (Message) argument;
            propagation.write(RequestContext.get(), location, new StringMapWriter(message.getProperties(), message::putUserProperty));
        } else if (argument instanceof Collection) {
            Collection<Message> messages = (Collection<Message>) argument;
            Carrier carrier = RequestContext.get();
            for (Message message : messages) {
                propagation.write(carrier, location, new StringMapWriter(message.getProperties(), message::putUserProperty));
            }
        }
    }
}
