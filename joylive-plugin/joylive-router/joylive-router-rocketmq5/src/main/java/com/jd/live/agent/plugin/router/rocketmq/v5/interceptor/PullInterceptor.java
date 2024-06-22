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
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.governance.interceptor.AbstractMQConsumerInterceptor;
import com.jd.live.agent.governance.invoke.InvocationContext;
import org.apache.rocketmq.client.consumer.PullCallback;
import org.apache.rocketmq.client.consumer.PullResult;
import org.apache.rocketmq.client.consumer.PullStatus;
import org.apache.rocketmq.client.impl.CommunicationMode;
import org.apache.rocketmq.common.message.MessageQueue;

import java.util.ArrayList;

/**
 * PullInterceptor
 *
 * @since 1.0.0
 */
public class PullInterceptor extends AbstractMQConsumerInterceptor {

    public PullInterceptor(InvocationContext context) {
        super(context);
    }

    /**
     * Enhanced logic before method execution. This method is called before the
     * target method is executed.
     *
     * @param ctx The execution context of the method being intercepted.
     * @see org.apache.rocketmq.client.impl.consumer.PullAPIWrapper#pullKernelImpl(MessageQueue, String, String, long, long, int, int, long, long, long, CommunicationMode, PullCallback)
     */
    @Override
    public void onEnter(ExecutableContext ctx) {
        if (isConsumeDisabled()) {
            Object[] arguments = ctx.getArguments();
            MethodContext mc = (MethodContext) ctx;
            PullResult result = new PullResult(PullStatus.NO_NEW_MSG, (Long) arguments[4],
                    0, 0, new ArrayList<>());
            mc.setResult(result);
            mc.setSkip(true);
        }
    }

}
