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
package com.jd.live.agent.plugin.transmission.okhttp.v1.request;

import com.jd.live.agent.core.util.type.FieldPath;
import com.jd.live.agent.governance.request.header.HeaderReader;
import com.jd.live.agent.governance.request.header.HeaderWriter;
import com.squareup.okhttp.Request;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BuilderParser implements HeaderReader, HeaderWriter {

    private static final FieldPath path = new FieldPath(Request.Builder.class, "headers.namesAndValues");

    private final List<String> namesAndValues;

    public BuilderParser(List<String> namesAndValues) {
        this.namesAndValues = namesAndValues;
    }

    @Override
    public Iterator<String> getNames() {
        List<String> names = new ArrayList<>(namesAndValues.size() / 2);
        int max = namesAndValues.size() - 2;
        for (int i = 0; i <= max; i += 2) {
            names.add(namesAndValues.get(i));
        }
        return names.iterator();
    }

    @Override
    public List<String> getHeaders(String key) {
        List<String> values = new ArrayList<>(2);
        int max = namesAndValues.size() - 2;
        for (int i = 0; i <= max; i += 2) {
            if (namesAndValues.get(i).equalsIgnoreCase(key)) {
                values.add(namesAndValues.get(i + 1));
            }
        }
        return values;
    }

    @Override
    public String getHeader(String key) {
        int max = namesAndValues.size() - 2;
        for (int i = 0; i <= max; i += 2) {
            if (namesAndValues.get(i).equalsIgnoreCase(key)) {
                return namesAndValues.get(i + 1);
            }
        }
        return null;
    }

    @Override
    public boolean isDuplicable() {
        return true;
    }

    @Override
    public void addHeader(String key, String value) {
        namesAndValues.add(key);
        namesAndValues.add(value);
    }

    @Override
    public void setHeader(String key, String value) {
        for (int i = namesAndValues.size() - 2; i >= 0; i -= 2) {
            if (namesAndValues.get(i).equalsIgnoreCase(key)) {
                namesAndValues.remove(i);
                namesAndValues.remove(i);
            }
        }
        namesAndValues.add(key);
        namesAndValues.add(value);
    }

    /**
     * Creates a new instance of {@link BuilderParser} using the names and values from the given {@link Request.Builder}.
     *
     * @param builder the Request.Builder from which to extract the names and values
     * @return a new instance of HeaderBuilderParser initialized with the names and values
     */
    @SuppressWarnings("unchecked")
    public static BuilderParser of(Request.Builder builder) {
        return new BuilderParser((List<String>) path.get(builder, ArrayList::new));
    }

}
