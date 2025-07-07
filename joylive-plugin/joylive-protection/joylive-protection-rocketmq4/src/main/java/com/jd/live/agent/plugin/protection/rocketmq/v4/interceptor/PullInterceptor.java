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
package com.jd.live.agent.plugin.protection.rocketmq.v4.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.bootstrap.util.type.FieldAccessor;
import com.jd.live.agent.governance.interceptor.AbstractMessageInterceptor;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.invoke.auth.Permission;
import org.apache.rocketmq.client.consumer.PullResult;
import org.apache.rocketmq.client.consumer.PullStatus;
import org.apache.rocketmq.client.impl.consumer.PullAPIWrapper;
import org.apache.rocketmq.client.impl.factory.MQClientInstance;
import org.apache.rocketmq.common.message.MessageQueue;

import java.util.ArrayList;

import static com.jd.live.agent.bootstrap.util.type.FieldAccessorFactory.getAccessor;

public class PullInterceptor extends AbstractMessageInterceptor {

    public PullInterceptor(InvocationContext context) {
        super(context);
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        PullAPIWrapper wrapper = (PullAPIWrapper) ctx.getTarget();
        MQClientInstance config = Accessors.mQClientFactory.get(wrapper, MQClientInstance.class);
        String address = config != null ? config.getClientConfig().getNamesrvAddr() : null;
        MessageQueue messageQueue = ctx.getArgument(0);
        Permission permission = isConsumeReady(messageQueue.getTopic(), address);
        if (!permission.isSuccess()) {
            ((MethodContext) ctx).skipWithResult(
                    new PullResult(PullStatus.NO_NEW_MSG, ctx.getArgument(4), 0, 0, new ArrayList<>()));
        }
    }

    private static class Accessors {
        private static final FieldAccessor mQClientFactory = getAccessor(PullAPIWrapper.class, "mQClientFactory");
    }
}
