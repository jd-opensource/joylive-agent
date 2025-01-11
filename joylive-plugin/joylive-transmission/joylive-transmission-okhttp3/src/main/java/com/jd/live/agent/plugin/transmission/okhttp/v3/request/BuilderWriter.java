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
package com.jd.live.agent.plugin.transmission.okhttp.v3.request;

import com.jd.live.agent.core.util.KeyValue;
import com.jd.live.agent.core.util.LookupIndex;
import com.jd.live.agent.core.util.type.FieldPath;
import com.jd.live.agent.governance.request.HeaderFeature;
import com.jd.live.agent.governance.request.HeaderWriter;
import okhttp3.Request;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.jd.live.agent.core.util.CollectionUtils.lookup;
import static java.util.Collections.sort;

public class BuilderWriter implements HeaderWriter {

    private static final FieldPath path = new FieldPath(Request.Builder.class, "headers.namesAndValues");

    private final List<String> namesAndValues;

    public BuilderWriter(List<String> namesAndValues) {
        this.namesAndValues = namesAndValues;
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
    public HeaderFeature getFeature() {
        return HeaderFeature.DUPLICABLE_BATCHABLE;
    }

    @Override
    public void addHeader(String key, String value) {
        namesAndValues.add(key);
        namesAndValues.add(value);
    }

    @Override
    public void setHeader(String key, String value) {
        // update single
        LookupIndex index = updateSingle(key, value, namesAndValues.size());
        if (index != null && index.size() > 1) {
            // keep only one
            List<Integer> indices = index.getIndices();
            int idx;
            for (int i = indices.size() - 1; i > 0; i--) {
                idx = indices.get(i);
                namesAndValues.remove(idx + 1);
                namesAndValues.remove(idx);
            }
            namesAndValues.set(indices.get(0), value);
        }
    }

    @Override
    public void setHeaders(Map<String, String> headers) {
        int length = namesAndValues.size();
        List<Integer> indices = null;
        List<KeyValue<String, String>> keyValues = null;
        String key;
        String value;
        LookupIndex index;
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            key = entry.getKey();
            value = entry.getValue();
            index = updateSingle(key, value, length);
            if (index != null && index.size() > 1) {
                if (indices == null) {
                    indices = new ArrayList<>(2);
                }
                if (keyValues == null) {
                    keyValues = new ArrayList<>(2);
                }
                indices.addAll(index.getIndices());
                keyValues.add(new KeyValue<>(key, value));
            }
        }
        if (indices != null && !indices.isEmpty()) {
            sort(indices);
            int idx;
            // remove duplicate keys
            for (int i = indices.size() - 1; i >= 0; i--) {
                idx = indices.get(i);
                namesAndValues.remove(idx + 1);
                namesAndValues.remove(idx);
            }
            // add new
            for (KeyValue<String, String> keyValue : keyValues) {
                namesAndValues.add(keyValue.getKey());
                namesAndValues.add(keyValue.getValue());
            }
        }
    }

    /**
     * Updates the value associated with a given key in the list of names and values.
     * If the key is not found, it adds the key and value to the list.
     * If the key is found once, it updates the value.
     * If the key is found multiple times, it returns the LookupIndex without updating.
     *
     * @param key    the key to update or add
     * @param value  the value to associate with the key
     * @param length the number of elements to consider in the list
     * @return the LookupIndex indicating the presence and positions of the key
     */
    private LookupIndex updateSingle(String key, String value, int length) {
        LookupIndex index = lookup(namesAndValues, length, 2, v -> v.equalsIgnoreCase(key));
        int size = index == null ? 0 : index.size();
        if (size == 0) {
            // add
            namesAndValues.add(key);
            namesAndValues.add(value);
        } else if (size == 1) {
            // most is only one
            namesAndValues.set(index.getIndex() + 1, value);
        }
        return index;
    }

    @SuppressWarnings("unchecked")
    public static BuilderWriter of(Request.Builder builder) {
        return new BuilderWriter((List<String>) path.get(builder));
    }

}
