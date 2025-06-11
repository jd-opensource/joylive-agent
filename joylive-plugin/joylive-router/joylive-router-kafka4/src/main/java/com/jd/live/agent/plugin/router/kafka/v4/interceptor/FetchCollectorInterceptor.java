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
package com.jd.live.agent.plugin.router.kafka.v4.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.governance.interceptor.AbstractMessageInterceptor;
import com.jd.live.agent.governance.invoke.InvocationContext;
import org.apache.kafka.clients.consumer.internals.Fetch;
import org.apache.kafka.common.TopicPartition;

import static com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessorFactory.getQuietly;

public class FetchCollectorInterceptor extends AbstractMessageInterceptor {

    private static final String FIELD_PARTITION = "partition";

    public FetchCollectorInterceptor(InvocationContext context) {
        super(context);
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        TopicPartition partition = getQuietly(ctx.getArgument(0), FIELD_PARTITION);
        if (partition != null && !isConsumeReady(partition.topic())) {
            MethodContext mc = (MethodContext) ctx;
            mc.setResult(Fetch.empty());
            mc.setSkip(true);
        }
    }
}
