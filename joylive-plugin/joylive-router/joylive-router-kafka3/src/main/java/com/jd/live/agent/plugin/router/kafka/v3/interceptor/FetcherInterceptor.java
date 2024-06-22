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
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.governance.interceptor.AbstractMQConsumerInterceptor;
import com.jd.live.agent.governance.invoke.InvocationContext;
import org.apache.kafka.clients.consumer.internals.Fetch;
import org.apache.kafka.clients.consumer.internals.Fetcher;

/**
 * FetcherInterceptor
 *
 * @since 1.0.0
 */
public class FetcherInterceptor extends AbstractMQConsumerInterceptor {

    public FetcherInterceptor(InvocationContext context) {
        super(context);
    }

    /**
     * Enhanced logic before method execution. This method is called before the
     * target method is executed.
     *
     * @param ctx The execution context of the method being intercepted.
     * @see org.apache.kafka.clients.consumer.internals.Fetcher #fetchRecords(Fetcher.CompletedFetch, int)
     */
    @Override
    public void onEnter(ExecutableContext ctx) {
        if (isConsumeDisabled()) {
            MethodContext mc = (MethodContext) ctx;
            mc.setResult(Fetch.empty());
            mc.setSkip(true);
        }

    }
}
