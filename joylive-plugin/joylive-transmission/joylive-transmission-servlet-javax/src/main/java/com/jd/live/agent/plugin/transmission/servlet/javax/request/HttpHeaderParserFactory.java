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

/**
 * An interface for creating instances of HttpHeaderParser.
 */
public interface HttpHeaderParserFactory {

    /**
     * Creates a new instance of HttpHeaderParser.
     *
     * @return a new instance of HttpHeaderParser
     */
    HttpHeaderParser create();

    /**
     * Checks if the factory supports creating HttpHeaderParser instances for the given type.
     *
     * @param type the type to check support for
     * @return true if the factory supports creating HttpHeaderParser instances for the given type, false otherwise
     */
    boolean support(Class<?> type);
}

