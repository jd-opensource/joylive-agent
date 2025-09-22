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
package com.jd.live.agent.plugin.transmission.springweb.v7.util;

import com.jd.live.agent.bootstrap.util.type.FieldAccessor;
import com.jd.live.agent.core.util.type.ClassUtils;
import org.springframework.http.HttpHeaders;

import static com.jd.live.agent.bootstrap.util.type.FieldAccessorFactory.getAccessor;

/**
 * Utility class for detecting Spring Cloud environment and load balancer configuration.
 */
public class CloudUtils {

    private static final Class<?> readonlyType = ClassUtils.loadClass("org.springframework.http.ReadOnlyHttpHeaders", HttpHeaders.class.getClassLoader());

    private static final FieldAccessor headersAccessor = getAccessor(HttpHeaders.class, "headers");

    /**
     * Creates writable copy of HTTP headers.
     *
     * @param headers source headers
     * @return writable headers instance
     */
    public static HttpHeaders writable(HttpHeaders headers) {
        return readonlyType.isInstance(headers) ? new HttpHeaders(headers) : headers;
    }
}
