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
import com.jd.live.agent.core.instance.Location;
import com.jd.live.agent.governance.interceptor.AbstractMQConsumerInterceptor;
import com.jd.live.agent.governance.invoke.InvocationContext;

public class ConstructorInterceptor extends AbstractMQConsumerInterceptor {

    public ConstructorInterceptor(InvocationContext context, boolean liveEnabled, boolean laneEnabled) {
        super(context, liveEnabled, laneEnabled);
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        Object[] arguments = ctx.getArguments();
        Location location = context.getApplication().getLocation();
        String consumerGroup = (String) arguments[1];
        String unit = location.getUnit();
        String lane = location.getLane();
        if (liveEnabled && unit != null && !unit.isEmpty()) {
            consumerGroup = consumerGroup + "_unit_" + unit;
        }
        if (laneEnabled && lane != null && !lane.isEmpty()) {
            consumerGroup = consumerGroup + "_lane_" + lane;
        }
        arguments[1] = consumerGroup;
    }
}
