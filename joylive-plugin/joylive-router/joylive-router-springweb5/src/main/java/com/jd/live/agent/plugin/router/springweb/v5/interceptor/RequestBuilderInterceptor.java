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
package com.jd.live.agent.plugin.router.springweb.v5.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.core.util.cache.LazyObject;
import com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessor;
import com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessorFactory;

import java.net.URI;

import static com.jd.live.agent.core.util.http.HttpUtils.newURI;

/**
 * RequestBuilderInterceptor
 */
@Deprecated
public class RequestBuilderInterceptor extends InterceptorAdaptor {

    private static final LazyObject<URIConstructor> cache = new LazyObject<>(() -> {
        try {
            return new URIConstructor();
        } catch (Throwable e) {
            return null;
        }
    });

    @Override
    public void onEnter(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        URIConstructor constructor = cache.get();
        if (constructor != null) {
            try {
                mc.skipWithResult(constructor.build(mc.getTarget()));
            } catch (IllegalAccessException ignored) {
            }
        }
    }

    /**
     * A private static class that provides a way to construct URI objects from a target object.
     */
    private static class URIConstructor {

        private final UnsafeFieldAccessor uriField;

        private final UnsafeFieldAccessor uriPathField;

        URIConstructor() throws ClassNotFoundException, NoSuchFieldException {
            Class<?> uriClass = Class.forName("org.springframework.http.server.reactive.DefaultServerHttpRequestBuilder");
            uriField = UnsafeFieldAccessorFactory.getAccessor(uriClass, "uri");
            uriPathField = UnsafeFieldAccessorFactory.getAccessor(uriClass, "uriPath");
        }

        /**
         * Builds a new URI object from the target object.
         *
         * @param target the target object
         * @return a new URI object
         * @throws IllegalAccessException if the fields cannot be accessed
         */
        public URI build(Object target) throws IllegalAccessException {
            URI uri = (URI) uriField.get(target);
            String path = (String) uriPathField.get(target);
            return path == null ? uri : newURI(uri, null, null, null, path);
        }
    }
}
