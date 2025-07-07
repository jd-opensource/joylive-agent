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
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.bootstrap.util.type.FieldAccessor;
import com.jd.live.agent.governance.interceptor.AbstractMessageInterceptor;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.invoke.auth.Permission;
import com.jd.live.agent.plugin.protection.kafka.v4.config.LiveFetchConfig;
import org.apache.kafka.clients.consumer.internals.CompletedFetch;
import org.apache.kafka.clients.consumer.internals.Fetch;
import org.apache.kafka.clients.consumer.internals.FetchCollector;
import org.apache.kafka.clients.consumer.internals.FetchConfig;
import org.apache.kafka.common.TopicPartition;

import static com.jd.live.agent.bootstrap.util.type.FieldAccessorFactory.getAccessor;

public class FetchCollectorFetchRecordsInterceptor extends AbstractMessageInterceptor {

    public FetchCollectorFetchRecordsInterceptor(InvocationContext context) {
        super(context);
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        FetchCollector<?, ?> fetchCollector = (FetchCollector<?, ?>) ctx.getTarget();
        FetchConfig fetchConfig = Accessor.fetchConfig.get(fetchCollector, FetchConfig.class);
        TopicPartition partition = Accessor.partition.get(ctx.getArgument(0), TopicPartition.class);
        if (partition != null && fetchConfig instanceof LiveFetchConfig) {
            LiveFetchConfig cfg = (LiveFetchConfig) fetchConfig;
            Permission permission = isConsumeReady(partition.topic(), null, cfg.getAddresses());
            if (!permission.isSuccess()) {
                ((MethodContext) ctx).skipWithResult(Fetch.empty());
            }
        }
    }

    private static class Accessor {
        private static final FieldAccessor partition = getAccessor(CompletedFetch.class, "partition");
        private static final FieldAccessor fetchConfig = getAccessor(FetchCollector.class, "fetchConfig");
    }
}
