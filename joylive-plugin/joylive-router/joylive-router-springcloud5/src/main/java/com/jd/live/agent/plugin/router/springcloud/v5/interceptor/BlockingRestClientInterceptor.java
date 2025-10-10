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
package com.jd.live.agent.plugin.router.springcloud.v5.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.plugin.router.springcloud.v5.request.BlockingClientHttpRequestBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.*;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.List;

import static com.jd.live.agent.bootstrap.util.type.FieldAccessorFactory.getQuietly;
import static com.jd.live.agent.bootstrap.util.type.FieldAccessorFactory.setValue;
import static com.jd.live.agent.core.util.type.ClassUtils.getDeclaredField;
import static com.jd.live.agent.core.util.type.ClassUtils.loadClass;

/**
 * BlockingRestClientInterceptor
 */
public class BlockingRestClientInterceptor extends AbstractBlockingClientInterceptor {

    public BlockingRestClientInterceptor(InvocationContext context) {
        super(context);
    }

    @Override
    protected BlockingClientHttpRequestBuilder builder(ExecutableContext ctx) {
        return new RestClientRequestBuilder(ctx);
    }

    /**
     * Implementation using RestClient for building HTTP requests.
     */
    private static class RestClientRequestBuilder implements BlockingClientHttpRequestBuilder {

        private static final Class<?> CLASS_DEFAULT_REST_CLIENT = loadClass("org.springframework.web.client.DefaultRestClient", RestClient.class.getClassLoader());

        private static final Field INITIALIZERS = getDeclaredField(CLASS_DEFAULT_REST_CLIENT, "initializers");

        private static final Field CLIENT_REQUEST_FACTORY = getDeclaredField(CLASS_DEFAULT_REST_CLIENT, "clientRequestFactory");

        private static final Field INTERCEPTORS = getDeclaredField(CLASS_DEFAULT_REST_CLIENT, "interceptors");

        private static final Field INTERCEPTING_REQUEST_FACTORY = getDeclaredField(CLASS_DEFAULT_REST_CLIENT, "interceptingRequestFactory");

        private static final Class<?> CLASS_DEFAULT_REQUEST_BODY_URI_SPEC = loadClass("org.springframework.web.client.DefaultRestClient$DefaultRequestBodyUriSpec", RestClient.class.getClassLoader());

        private static final Field HTTP_METHOD = getDeclaredField(CLASS_DEFAULT_REQUEST_BODY_URI_SPEC, "httpMethod");

        private static final Field URI = getDeclaredField(CLASS_DEFAULT_REQUEST_BODY_URI_SPEC, "uri");

        private static final Field PARENT = getDeclaredField(CLASS_DEFAULT_REQUEST_BODY_URI_SPEC, "this$0");

        private final RestClient client;

        private final URI uri;

        private final HttpMethod method;

        private final ClientHttpRequestFactory factory;

        RestClientRequestBuilder(ExecutableContext ctx) {
            this(getRestClient(ctx.getTarget()), getUri(ctx.getTarget()), getMethod(ctx.getTarget()));
        }

        RestClientRequestBuilder(RestClient client, URI uri, HttpMethod method) {
            this.client = client;
            this.factory = getFactory(client);
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
            return getInterceptors(client);
        }

        @Override
        public void initialize(ClientHttpRequest request) {
            List<ClientHttpRequestInitializer> initializers = getQuietly(client, INITIALIZERS);
            if (initializers != null) {
                for (ClientHttpRequestInitializer initializer : initializers) {
                    initializer.initialize(request);
                }
            }
        }

        @Override
        public ClientHttpRequest create(URI uri, HttpMethod method) throws IOException {
            return factory.createRequest(uri, method);
        }

        /**
         * Gets interceptors from the REST client.
         *
         * @param client the REST client
         * @return list of interceptors
         */
        private static List<ClientHttpRequestInterceptor> getInterceptors(RestClient client) {
            return getQuietly(client, INTERCEPTORS);
        }

        /**
         * Gets the request factory from the REST client.
         *
         * @param client the REST client
         * @return the client HTTP request factory
         */
        private static ClientHttpRequestFactory getFactory(RestClient client) {
            ClientHttpRequestFactory factory = getQuietly(client, CLIENT_REQUEST_FACTORY);
            List<ClientHttpRequestInterceptor> interceptors = getQuietly(client, INTERCEPTORS);
            if (interceptors != null) {
                factory = getQuietly(client, INTERCEPTING_REQUEST_FACTORY);
                if (factory == null) {
                    factory = new InterceptingClientHttpRequestFactory(factory, interceptors);
                    setValue(factory, INTERCEPTING_REQUEST_FACTORY, client);
                }
            }
            return factory;
        }

        /**
         * Extracts REST client from request specification.
         *
         * @param spec the request specification
         * @return the REST client
         */
        private static RestClient getRestClient(Object spec) {
            return getQuietly(spec, PARENT);
        }

        /**
         * Extracts URI from request specification.
         *
         * @param spec the request specification
         * @return the URI
         */
        private static URI getUri(Object spec) {
            return getQuietly(spec, URI);
        }

        /**
         * Extracts HTTP method from request specification.
         *
         * @param spec the request specification
         * @return the HTTP method
         */
        private static HttpMethod getMethod(Object spec) {
            return getQuietly(spec, HTTP_METHOD);
        }
    }
}
