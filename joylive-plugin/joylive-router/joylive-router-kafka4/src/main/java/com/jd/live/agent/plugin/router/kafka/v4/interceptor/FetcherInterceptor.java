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
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessor;
import com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessorFactory;
import com.jd.live.agent.governance.interceptor.AbstractMessageInterceptor;
import com.jd.live.agent.governance.invoke.InvocationContext;
import org.apache.kafka.clients.consumer.internals.Fetch;
import org.apache.kafka.common.TopicPartition;

public class FetcherInterceptor extends AbstractMessageInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(FetcherInterceptor.class);
    private static final String TYPE_COMPLETED_FETCH = "org.apache.kafka.clients.consumer.internals.Fetcher.CompletedFetch";
    private static final String FIELD_PARTITION = "partition";

    private UnsafeFieldAccessor accessor;

    public FetcherInterceptor(InvocationContext context) {
        super(context);
        try {
            Class<?> type = Class.forName(TYPE_COMPLETED_FETCH);
            accessor = UnsafeFieldAccessorFactory.getAccessor(type, FIELD_PARTITION);
        } catch (Throwable e) {
            logger.error("Error occurs while accessing partition field of CompletedFetch.", e);
        }
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        if (accessor != null) {
            try {
                TopicPartition partition = (TopicPartition) accessor.get(ctx.getArgument(0));
                if (!isConsumeReady(partition.topic())) {
                    MethodContext mc = (MethodContext) ctx;
                    mc.setResult(Fetch.empty());
                    mc.setSkip(true);
                }
            } catch (Throwable e) {
                logger.error(e.getMessage(), e);
            }
        }
    }
}
