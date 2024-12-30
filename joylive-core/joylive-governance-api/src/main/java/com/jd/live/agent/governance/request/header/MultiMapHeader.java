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
package com.jd.live.agent.governance.request.header;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * A class that implements both {@link HeaderReader} and {@link HeaderWriter} interfaces.
 * This class is designed to handle the reading and writing of multi-map headers.
 */
public class MultiMapHeader implements HeaderReader, HeaderWriter {

    private final Map<String, List<String>> map;

    private final BiConsumer<String, String> consumer;

    public MultiMapHeader(Map<String, List<String>> map, BiConsumer<String, String> consumer) {
        this.map = map;
        this.consumer = consumer;
    }

    @Override
    public Iterator<String> getHeaderNames() {
        return map.keySet().iterator();
    }

    @Override
    public String getHeader(String key) {
        return HeaderReader.super.getHeader(key);
    }

    @Override
    public List<String> getHeaders(String key) {
        return map.get(key) == null ? null : map.get(key);
    }

    @Override
    public void setHeader(String key, String value) {
        consumer.accept(key, value);
    }
}
