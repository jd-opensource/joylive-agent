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
package com.jd.live.agent.plugin.router.rocketmq.v4.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.core.Constants;
import com.jd.live.agent.governance.interceptor.AbstractMQConsumerInterceptor;
import com.jd.live.agent.governance.invoke.InvocationContext;
import org.apache.rocketmq.client.hook.FilterMessageContext;
import org.apache.rocketmq.client.hook.FilterMessageHook;
import org.apache.rocketmq.common.message.MessageExt;

import java.util.ArrayList;
import java.util.List;

import static com.jd.live.agent.core.util.CollectionUtils.filter;

public class RegisterFilterInterceptor extends AbstractMQConsumerInterceptor {

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
                filter(filterContext.getMsgList(), RegisterFilterInterceptor.this::isAllow);
            }
        });
        arguments[0] = result;
    }

    /**
     * Determines whether the message is allowed based on live and lane checks.
     *
     * @param message the kafka message to check.
     * @return {@code true} if the lane checks allow the message; {@code false} otherwise.
     */
    private boolean isAllow(MessageExt message) {
        return isAllowLive(message) && isAllowLane(message);
    }

    /**
     * Determines whether the message is allowed based on lane checks.
     *
     * @param message the RocketMQ message to check.
     * @return {@code true} if the lane checks allow the message; {@code false} otherwise.
     */
    private boolean isAllowLane(MessageExt message) {
        if (!context.isLaneEnabled()) {
            return true;
        }
        String laneSpaceId = message.getUserProperty(Constants.LABEL_LANE_SPACE_ID);
        String lane = message.getUserProperty(Constants.LABEL_LANE);
        return allowLane(laneSpaceId, lane);
    }

    /**
     * Determines whether the message is allowed based on live checks.
     *
     * @param message the RocketMQ message to check.
     * @return {@code true} if the live checks allow the message; {@code false} otherwise.
     */
    private boolean isAllowLive(MessageExt message) {
        if (!context.isLiveEnabled()) {
            return true;
        }
        String liveSpaceId = message.getUserProperty(Constants.LABEL_LIVE_SPACE_ID);
        String ruleId = message.getUserProperty(Constants.LABEL_RULE_ID);
        String uid = message.getUserProperty(Constants.LABEL_VARIABLE);
        return allowLive(liveSpaceId, ruleId, uid);
    }
}
