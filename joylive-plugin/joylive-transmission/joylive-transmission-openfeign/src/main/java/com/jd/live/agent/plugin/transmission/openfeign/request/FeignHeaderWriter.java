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
package com.jd.live.agent.plugin.transmission.openfeign.request;

import com.jd.live.agent.core.util.CollectionUtils;
import com.jd.live.agent.governance.request.HeaderWriter;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.jd.live.agent.core.util.CollectionUtils.toList;

public class FeignHeaderWriter implements HeaderWriter {

    private final Map<String, Collection<String>> headers;

    public FeignHeaderWriter(Map<String, Collection<String>> headers) {
        this.headers = headers;
    }

    @Override
    public List<String> getHeaders(String key) {
        return headers.get(key) == null ? null : toList(headers.get(key));
    }

    @Override
    public String getHeader(String key) {
        Collection<String> values = headers.get(key);
        return values == null || values.isEmpty() ? null : values.iterator().next();
    }

    @Override
    public void addHeader(String key, String value) {
        Collection<String> values = headers.get(key);
        if (values == null) {
            headers.put(key, CollectionUtils.toList(value));
        } else {
            values.add(value);
        }
    }

    @Override
    public void setHeader(String key, String value) {
        headers.put(key, toList(value));
    }
}
