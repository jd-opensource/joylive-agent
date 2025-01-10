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
package com.jd.live.agent.plugin.transmission.jdkhttp.request;

import com.jd.live.agent.core.util.type.FieldPath;
import com.jd.live.agent.governance.request.HeaderParser;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

public class BuilderParser implements HeaderParser {

    private static final FieldPath path = new FieldPath("jdk.internal.net.http.HttpRequestBuilderImpl", "headersBuilder.headersMap");

    private final TreeMap<String, List<String>> headers;

    public BuilderParser(TreeMap<String, List<String>> headers) {
        this.headers = headers;
    }

    @Override
    public Iterator<String> getNames() {
        return headers.keySet().iterator();
    }

    @Override
    public List<String> getHeaders(String key) {
        return headers.get(key);
    }

    @Override
    public String getHeader(String key) {
        List<String> values = headers.get(key);
        return values == null || values.isEmpty() ? null : values.get(0);
    }

    @Override
    public boolean isDuplicable() {
        return true;
    }

    @Override
    public void addHeader(String key, String value) {
        headers.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
    }

    @Override
    public void setHeader(String key, String value) {
        List<String> values = new ArrayList<>(1);
        values.add(value);
        headers.put(key, values);
    }

    /**
     * Creates a new instance of {@link BuilderParser} using the names and values from the given builder object.
     *
     * @param builder the builder object from which to extract the names and values
     * @return a new instance of BuilderParser initialized with the names and values
     */
    @SuppressWarnings("unchecked")
    public static BuilderParser of(Object builder) {
        return new BuilderParser((TreeMap<String, List<String>>) path.get(builder, TreeMap::new));
    }

}
