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
package com.jd.live.agent.plugin.protection.rocketmq.v5.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.governance.interceptor.AbstractMessageInterceptor;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.invoke.auth.Permission;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.impl.producer.DefaultMQProducerImpl;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.protocol.ResponseCode;

public class SendInterceptor extends AbstractMessageInterceptor {

    public SendInterceptor(InvocationContext context) {
        super(context);
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        Message message = ctx.getArgument(0);
        DefaultMQProducerImpl producerImpl = (DefaultMQProducerImpl) ctx.getTarget();
        String address = producerImpl.getDefaultMQProducer().getNamesrvAddr();
        Permission permission = isProduceReady(message.getTopic(), address);
        if (!permission.isSuccess()) {
            mc.skipWithThrowable(new MQClientException(ResponseCode.NO_PERMISSION, permission.getMessage()));
        }
    }
}
