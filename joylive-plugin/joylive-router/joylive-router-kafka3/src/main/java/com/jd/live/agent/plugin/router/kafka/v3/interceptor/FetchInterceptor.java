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
package com.jd.live.agent.plugin.router.kafka.v3.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.core.Constants;
import com.jd.live.agent.governance.interceptor.AbstractMQConsumerInterceptor;
import com.jd.live.agent.governance.invoke.InvocationContext;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;

import java.util.Arrays;
import java.util.List;

import static com.jd.live.agent.core.util.CollectionUtils.filter;

public class FetchInterceptor extends AbstractMQConsumerInterceptor {

    public FetchInterceptor(InvocationContext context) {
        super(context);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onEnter(ExecutableContext ctx) {
        Object[] arguments = ctx.getArguments();
        List<ConsumerRecord<?, ?>> records = (List<ConsumerRecord<?, ?>>) arguments[1];
        filter(records, this::isAllow);
    }

    /**
     * Determines whether the message is allowed based on live and lane checks.
     *
     * @param message the kafka message to check.
     * @return {@code true} if the lane checks allow the message; {@code false} otherwise.
     */
    private boolean isAllow(ConsumerRecord<?, ?> message) {
        return isAllowLive(message) && isAllowLane(message);
    }

    /**
     * Determines whether the message is allowed based on lane checks.
     *
     * @param message the kafka message to check.
     * @return {@code true} if the lane checks allow the message; {@code false} otherwise.
     */
    private boolean isAllowLane(ConsumerRecord<?, ?> message) {
        if (!context.isLaneEnabled()) {
            return true;
        }
        String laneSpaceId = getHeader(message, Constants.LABEL_LANE_SPACE_ID);
        String lane = getHeader(message, Constants.LABEL_LANE);
        return allowLane(laneSpaceId, lane);
    }

    /**
     * Determines whether the message is allowed based on live checks.
     *
     * @param message the kafka message to check.
     * @return {@code true} if the live checks allow the message; {@code false} otherwise.
     */
    private boolean isAllowLive(ConsumerRecord<?, ?> message) {
        if (!context.isLiveEnabled()) {
            return true;
        }
        String liveSpaceId = getHeader(message, Constants.LABEL_LIVE_SPACE_ID);
        String ruleId = getHeader(message, Constants.LABEL_RULE_ID);
        String uid = getHeader(message, Constants.LABEL_VARIABLE);
        return allowLive(liveSpaceId, ruleId, uid);
    }

    private String getHeader(ConsumerRecord<?, ?> message, String key) {
        Header header = message.headers().lastHeader(key);
        return header == null ? null : Arrays.toString(header.value());
    }
}
