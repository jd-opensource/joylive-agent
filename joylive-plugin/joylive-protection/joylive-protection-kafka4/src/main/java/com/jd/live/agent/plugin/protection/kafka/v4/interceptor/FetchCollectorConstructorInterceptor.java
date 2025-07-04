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
package com.jd.live.agent.plugin.protection.kafka.v4.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.plugin.protection.kafka.v4.config.LiveFetchConfig;
import com.jd.live.agent.plugin.protection.kafka.v4.context.KafkaContext;
import org.apache.kafka.clients.consumer.ConsumerConfig;

import static com.jd.live.agent.core.util.StringUtils.split;

public class FetchCollectorConstructorInterceptor extends InterceptorAdaptor {

    @Override
    public void onEnter(ExecutableContext ctx) {
        ConsumerConfig config = KafkaContext.remove();
        if (config != null) {
            ctx.setArgument(3, new LiveFetchConfig(ctx.getArgument(3),
                    split(config.getString(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG))));
        }
    }

}
