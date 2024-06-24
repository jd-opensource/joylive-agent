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
package com.jd.live.agent.plugin.router.rocketmq.v5.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.governance.interceptor.AbstractMQConsumerInterceptor;
import com.jd.live.agent.governance.invoke.InvocationContext;
import org.apache.rocketmq.common.message.Message;

import java.util.Collection;

public class SendInterceptor extends AbstractMQConsumerInterceptor {

    public SendInterceptor(InvocationContext context) {
        super(context);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onEnter(ExecutableContext ctx) {
        Object[] arguments = ctx.getArguments();
        if (arguments[0] instanceof Message) {
            updateTopic((Message) arguments[0]);
        } else if (arguments[0] instanceof Collection) {
            Collection<Message> messages = (Collection<Message>) arguments[0];
            for (Message message : messages) {
                updateTopic(message);
            }
        }
    }

    private void updateTopic(Message message) {
        String topic = message.getTopic();
        if (isEnabled(topic)) {
            message.setTopic(context.getTopicConverter().getTarget(topic));
        }
    }
}
