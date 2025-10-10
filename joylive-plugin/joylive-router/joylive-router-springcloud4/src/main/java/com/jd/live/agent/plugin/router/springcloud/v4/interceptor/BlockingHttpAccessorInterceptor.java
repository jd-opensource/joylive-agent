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
package com.jd.live.agent.plugin.router.springcloud.v4.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.plugin.router.springcloud.v4.request.BlockingClientHttpRequestBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.support.HttpAccessor;
import org.springframework.http.client.support.InterceptingHttpAccessor;

import java.io.IOException;
import java.net.URI;
import java.util.List;

/**
 * BlockingClientInterceptor
 */
public class BlockingHttpAccessorInterceptor extends AbstractBlockingClientInterceptor {

    public BlockingHttpAccessorInterceptor(InvocationContext context) {
        super(context);
    }

    @Override
    protected BlockingClientHttpRequestBuilder builder(ExecutableContext ctx) {
        return new HttpAccessorRequestBuilder(ctx);
    }

    /**
     * Implementation using HttpAccessor for building HTTP requests.
     */
    private static class HttpAccessorRequestBuilder implements BlockingClientHttpRequestBuilder {

        private final HttpAccessor template;
        private final URI uri;
        private final HttpMethod method;

        HttpAccessorRequestBuilder(ExecutableContext ctx) {
            this((HttpAccessor) ctx.getTarget(), ctx.getArgument(0), ctx.getArgument(1));
        }

        HttpAccessorRequestBuilder(HttpAccessor template, URI uri, HttpMethod method) {
            this.template = template;
            this.uri = uri;
            this.method = method;
        }

        @Override
        public URI getUri() {
            return uri;
        }

        @Override
        public HttpMethod getMethod() {
            return method;
        }

        @Override
        public List<ClientHttpRequestInterceptor> getInterceptors() {
            return template instanceof InterceptingHttpAccessor ? ((InterceptingHttpAccessor) template).getInterceptors() : null;
        }

        @Override
        public ClientHttpRequest create(URI uri, HttpMethod method) throws IOException {
            return template.getRequestFactory().createRequest(uri, method);
        }

        @Override
        public void initialize(ClientHttpRequest request) {
            template.getClientHttpRequestInitializers().forEach(initializer -> initializer.initialize(request));
        }
    }
}
