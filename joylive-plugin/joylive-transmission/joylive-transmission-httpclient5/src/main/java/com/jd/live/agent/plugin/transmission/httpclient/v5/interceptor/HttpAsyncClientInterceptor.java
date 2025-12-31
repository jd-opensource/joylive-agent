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
package com.jd.live.agent.plugin.transmission.httpclient.v5.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.context.RequestContext;
import com.jd.live.agent.governance.context.bag.Carrier;
import com.jd.live.agent.governance.context.bag.Propagation;
import com.jd.live.agent.plugin.transmission.httpclient.v5.request.HttpMessageWriter;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.nio.AsyncRequestProducer;
import org.apache.hc.core5.http.nio.DataStreamChannel;
import org.apache.hc.core5.http.nio.RequestChannel;
import org.apache.hc.core5.http.protocol.HttpContext;

import java.io.IOException;

public class HttpAsyncClientInterceptor extends InterceptorAdaptor {

    private final Propagation propagation;

    private final int argument;

    public HttpAsyncClientInterceptor(Propagation propagation, int argument) {
        this.propagation = propagation;
        this.argument = argument;
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        ctx.setArgument(argument, new LiveAsyncRequestProducer(ctx.getArgument(argument), propagation, RequestContext.get()));
    }

    private static class LiveAsyncRequestProducer implements AsyncRequestProducer {

        private final AsyncRequestProducer delegate;

        private final Propagation propagation;

        private final Carrier carrier;

        LiveAsyncRequestProducer(AsyncRequestProducer delegate, Propagation propagation, Carrier carrier) {
            this.delegate = delegate;
            this.propagation = propagation;
            this.carrier = carrier;
        }

        @Override
        public void sendRequest(RequestChannel channel, HttpContext context) throws HttpException, IOException {
            delegate.sendRequest(new LiveRequestChannel(channel, propagation, carrier), context);
        }

        @Override
        public boolean isRepeatable() {
            return delegate.isRepeatable();
        }

        @Override
        public void failed(Exception cause) {
            delegate.failed(cause);
        }

        @Override
        public int available() {
            return delegate.available();
        }

        @Override
        public void produce(DataStreamChannel channel) throws IOException {
            delegate.produce(channel);
        }

        @Override
        public void releaseResources() {
            delegate.releaseResources();
        }
    }

    private static class LiveRequestChannel implements RequestChannel {

        private final RequestChannel delegate;

        private final Propagation propagation;

        private final Carrier carrier;

        LiveRequestChannel(RequestChannel delegate, Propagation propagation, Carrier carrier) {
            this.delegate = delegate;
            this.propagation = propagation;
            this.carrier = carrier;
        }

        @Override
        public void sendRequest(HttpRequest request, EntityDetails entityDetails, HttpContext context) throws HttpException, IOException {
            propagation.write(carrier, new HttpMessageWriter(request));
            delegate.sendRequest(request, entityDetails, context);
        }
    }

}
