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
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.core.util.tag.Label;
import com.jd.live.agent.governance.context.RequestContext;
import com.jd.live.agent.governance.context.bag.CargoRequire;
import com.jd.live.agent.governance.context.bag.CargoRequires;
import com.jd.live.agent.governance.context.bag.Carrier;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;

import java.util.List;

public class MessageInterceptor extends InterceptorAdaptor {

    private final CargoRequire require;

    public MessageInterceptor(List<CargoRequire> requires) {
        this.require = new CargoRequires(requires);
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        Boolean isProducer = RequestContext.getAttribute(Carrier.ATTRIBUTE_MQ_PRODUCER);
        if (isProducer == null || !isProducer) {
            restoreCargo((Message) ctx.getTarget());
        }
    }

    private void restoreCargo(Message message) {
        String messageId = message instanceof MessageExt ? ((MessageExt) message).getMsgId() : null;
        String id = "Rocketmq5@" + message.getTopic() + "@" + messageId;
        RequestContext.restore(() -> id,
                carrier -> carrier.addCargo(require, message.getProperties(), Label::parseValue));
    }

}
