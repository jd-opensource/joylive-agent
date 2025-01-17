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

import com.jd.live.agent.governance.request.HeaderReader;

import javax.servlet.http.HttpServletRequest;
import java.util.Iterator;
import java.util.List;

import static com.jd.live.agent.core.util.CollectionUtils.toIterator;
import static com.jd.live.agent.core.util.CollectionUtils.toList;

public class HttpServletRequestParser implements HeaderReader {

    private final HttpServletRequest request;

    public HttpServletRequestParser(HttpServletRequest request) {
        this.request = request;
    }

    @Override
    public Iterator<String> getNames() {
        return toIterator(request.getHeaderNames());
    }

    @Override
    public List<String> getHeaders(String key) {
        return toList(request.getHeaders(key));
    }

    @Override
    public String getHeader(String key) {
        return request.getHeader(key);
    }
}
