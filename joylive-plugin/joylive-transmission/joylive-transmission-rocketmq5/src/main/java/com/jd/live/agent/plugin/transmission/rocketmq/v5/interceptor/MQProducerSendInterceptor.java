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
import com.jd.live.agent.governance.context.RequestContext;
import org.apache.rocketmq.common.message.Message;

import java.util.Collection;

public class MQProducerSendInterceptor extends InterceptorAdaptor {

    private static final char SPLITTER_KEY_VALUE = 1;

    private static final char SPLITTER_HEADER = 2;

    @SuppressWarnings("unchecked")
    @Override
    public void onEnter(ExecutableContext ctx) {
        Object argument = ctx.getArguments()[0];
        if (argument instanceof Message) {
            attachTag((Message) argument);
        } else if (argument instanceof Collection) {
            attachTag((Collection<Message>) argument);
        }
    }

    private void attachTag(Collection<Message> messages) {
        RequestContext.cargos(cargo ->
                messages.forEach(
                        message -> message.putUserProperty(cargo.getKey(), cargo.getValue())));
    }

    private void attachTag(Message message) {
        RequestContext.cargos(cargo -> message.putUserProperty(cargo.getKey(), cargo.getValue()));
    }
}
