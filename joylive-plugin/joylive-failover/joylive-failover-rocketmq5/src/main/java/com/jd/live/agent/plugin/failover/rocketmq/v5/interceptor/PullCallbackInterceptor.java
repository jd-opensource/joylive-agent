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
package com.jd.live.agent.plugin.failover.rocketmq.v5.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import org.apache.rocketmq.client.consumer.PullCallback;
import org.apache.rocketmq.client.impl.consumer.DefaultMQPushConsumerImpl;

import static com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessorFactory.getQuietly;

/**
 * PullCallbackInterceptor
 */
public class PullCallbackInterceptor extends InterceptorAdaptor {

    @Override
    public void onEnter(ExecutableContext ctx) {
        Object target = ctx.getTarget();
        if (target instanceof PullCallback) {
            MethodContext mc = (MethodContext) ctx;
            DefaultMQPushConsumerImpl consumerImpl = getQuietly(target, "this$0");
            switch (consumerImpl.getServiceState()) {
                case SHUTDOWN_ALREADY:
                case START_FAILED:
                    // skip exception when failover.
                    mc.skip();
            }
        }
    }
}
