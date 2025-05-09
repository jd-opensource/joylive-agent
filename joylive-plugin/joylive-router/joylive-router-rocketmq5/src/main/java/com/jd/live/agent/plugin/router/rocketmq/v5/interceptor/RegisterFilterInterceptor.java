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
import com.jd.live.agent.governance.interceptor.AbstractMessageInterceptor;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.plugin.router.rocketmq.v5.message.RocketMQMessage;
import org.apache.rocketmq.client.hook.FilterMessageContext;
import org.apache.rocketmq.client.hook.FilterMessageHook;

import java.util.ArrayList;
import java.util.List;

import static com.jd.live.agent.core.util.CollectionUtils.filter;

public class RegisterFilterInterceptor extends AbstractMessageInterceptor {

    public RegisterFilterInterceptor(InvocationContext context) {
        super(context);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onEnter(ExecutableContext ctx) {
        Object[] arguments = ctx.getArguments();
        List<FilterMessageHook> hooks = (List<FilterMessageHook>) arguments[0];
        List<FilterMessageHook> result = hooks == null ? new ArrayList<>() : new ArrayList<>(hooks);
        result.add(new FilterMessageHook() {
            @Override
            public String hookName() {
                return "live-filter";
            }

            @Override
            public void filterMessage(FilterMessageContext filterContext) {
                filter(filterContext.getMsgList(), message -> consume(new RocketMQMessage(message)) == MessageAction.CONSUME);
            }
        });
        arguments[0] = result;
    }
}
