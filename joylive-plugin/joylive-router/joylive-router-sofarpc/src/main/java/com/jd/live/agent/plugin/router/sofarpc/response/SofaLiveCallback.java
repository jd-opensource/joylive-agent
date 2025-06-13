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
package com.jd.live.agent.plugin.router.sofarpc.response;

import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.invoke.SofaResponseCallback;
import com.alipay.sofa.rpc.core.request.RequestBase;

import java.util.concurrent.CompletableFuture;

/**
 * A delegating {@link SofaResponseCallback} that bridges callback results to a {@link CompletableFuture}.
 *
 * <p>Forwards all callback events to both the original delegate callback and the attached future:
 * <ul>
 *   <li>Success responses complete the future normally</li>
 *   <li>Exceptions (application or SOFA) complete the future exceptionally</li>
 * </ul>
 */
public class SofaLiveCallback implements SofaResponseCallback<Object> {

    public static final ThreadLocal<CompletableFuture<Object>> FUTURE = new ThreadLocal<>();

    private final SofaResponseCallback<?> delegate;
    private final CompletableFuture<Object> future;

    public SofaLiveCallback(SofaResponseCallback<?> delegate) {
        this(delegate, new CompletableFuture<>());
    }

    public SofaLiveCallback(SofaResponseCallback<?> delegate, CompletableFuture<Object> future) {
        this.delegate = delegate;
        this.future = future;
    }

    @Override
    public void onAppResponse(Object appResponse, String methodName, RequestBase request) {
        future.complete(appResponse);
        if (delegate != null) {
            delegate.onAppResponse(appResponse, methodName, request);
        }
    }

    @Override
    public void onAppException(Throwable throwable, String methodName, RequestBase request) {
        future.completeExceptionally(throwable);
        if (delegate != null) {
            delegate.onAppException(throwable, methodName, request);
        }
    }

    @Override
    public void onSofaException(SofaRpcException sofaException, String methodName, RequestBase request) {
        future.completeExceptionally(sofaException);
        if (delegate != null) {
            delegate.onSofaException(sofaException, methodName, request);
        }
    }

    public CompletableFuture<Object> getFuture() {
        return future;
    }
}
