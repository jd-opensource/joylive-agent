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
package com.jd.live.agent.plugin.transmission.servlet.javax.request;

import com.jd.live.agent.plugin.transmission.servlet.javax.request.tomcat.TomcatHeaderParserFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * A utility class for creating instances of HttpHeaderParser based on the type of the request object.
 */
public class HttpHeaderParsers {

    private static final HttpHeaderParserFactory[] factories;

    static {
        List<HttpHeaderParserFactory> validates = new ArrayList<>();
        if (TomcatHeaderParserFactory.isValid()) {
            validates.add(new TomcatHeaderParserFactory());
        }
        factories = validates.toArray(new HttpHeaderParserFactory[0]);
    }

    /**
     * Creates a new instance of HttpHeaderParser based on the type of the given request object.
     *
     * @param type the type of the request object
     * @return a new instance of HttpHeaderParser, or null if no suitable factory is found
     */
    public static HttpHeaderParser create(Class<?> type) {
        for (HttpHeaderParserFactory factory : factories) {
            if (factory.support(type)) {
                return factory.create();
            }
        }
        return null;
    }

}
