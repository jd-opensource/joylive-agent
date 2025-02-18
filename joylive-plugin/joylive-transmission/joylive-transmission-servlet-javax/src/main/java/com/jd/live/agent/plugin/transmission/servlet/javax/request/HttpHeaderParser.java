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

import com.jd.live.agent.core.util.map.MultiMap;

/**
 * An interface for parsing HTTP headers from a given request object.
 */
public interface HttpHeaderParser {

    /**
     * Parses the HTTP headers from the given request object and returns them as a MultiMap.
     *
     * @param request the request object to parse
     * @return a MultiMap containing the parsed HTTP headers
     */
    MultiMap<String, String> parse(Object request);
}
