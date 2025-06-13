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
package com.jd.live.agent.plugin.router.sofarpc.interceptor;

import com.alipay.sofa.rpc.core.invoke.SofaResponseCallback;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.message.AbstractResponseFuture;
import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.plugin.router.sofarpc.response.SofaLiveCallback;

import java.util.concurrent.CompletableFuture;

import static com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessorFactory.getQuietly;

/**
 * ResponseFutureNotifyListenersInterceptor
 */
public class ResponseFutureNotifyListenersInterceptor extends InterceptorAdaptor {

    private static final String FIELD_REQUEST = "request";
    private static final String FIELD_CAUSE = "cause";
    private static final String FIELD_RESULT = "result";

    @Override
    public void onEnter(ExecutableContext ctx) {
        AbstractResponseFuture<?> responseFuture = (AbstractResponseFuture<?>) ctx.getTarget();
        SofaRequest request = getQuietly(responseFuture, FIELD_REQUEST);
        if (request != null) {
            SofaResponseCallback<?> callback = request.getSofaResponseCallback();
            if (callback instanceof SofaLiveCallback) {
                // remove callback to prevent duplicated call.
                request.setSofaResponseCallback(null);
                CompletableFuture<Object> future = ((SofaLiveCallback) callback).getFuture();
                Throwable cause = getQuietly(responseFuture, FIELD_CAUSE);
                if (cause != null) {
                    future.completeExceptionally(cause);
                } else {
                    future.complete(getQuietly(responseFuture, FIELD_RESULT));
                }
            }
        }
    }
}
