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
package com.jd.live.agent.governance.policy.service.circuitbreak;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * DegradeConfig
 *
 * @since 1.1.0
 */
public class DegradeConfig {

    public static final String TYPE_STRING = "string";
    public static final String TYPE_APPLICATION_TEXT = "application/text";
    public static final String TYPE_APPLICATION_JSON = "application/json";
    public static final String TYPE_JSON = "json";

    @Getter
    @Setter
    private int responseCode;

    @Setter
    private String contentType;

    @Getter
    @Setter
    private Map<String, String> attributes;

    @Getter
    @Setter
    private String responseBody;

    private transient volatile byte[] responseBytes;

    public DegradeConfig() {
    }

    @Builder
    public DegradeConfig(int responseCode, String contentType, Map<String, String> attributes, String responseBody) {
        this.responseCode = responseCode;
        this.contentType = contentType;
        this.attributes = attributes;
        this.responseBody = responseBody;
    }

    public DegradeConfig(DegradeConfig config) {
        this.responseCode = config.responseCode;
        this.contentType = config.contentType;
        this.attributes = config.attributes == null ? null : new HashMap<>(config.attributes);
        this.responseBody = config.responseBody;
    }

    public String getContentType() {
        return contentType == null ? TYPE_APPLICATION_JSON : contentType;
    }

    public void foreach(BiConsumer<String, String> consumer) {
        if (attributes != null && !attributes.isEmpty() && consumer != null) {
            attributes.forEach(consumer);
        }
    }

    public void append(Map<String, Collection<String>> headers) {
        if (headers != null) {
            foreach((key, value) -> headers.computeIfAbsent(key, v -> new ArrayList<>()).add(value));
        }
    }

    public int getBodyLength() {
        return responseBody == null ? 0 : responseBody.length();
    }

    public boolean isText() {
        return TYPE_STRING.equalsIgnoreCase(contentType)
                || TYPE_JSON.equalsIgnoreCase(contentType)
                || TYPE_APPLICATION_TEXT.equalsIgnoreCase(contentType)
                || TYPE_APPLICATION_JSON.equalsIgnoreCase(contentType);
    }

    public byte[] getResponseBytes() {
        if (responseBytes == null) {
            synchronized (this) {
                if (responseBytes == null) {
                    responseBytes = responseBody == null ? new byte[0] : responseBody.getBytes();
                }
            }
        }
        return responseBytes;
    }

    protected void cache() {
        if (attributes != null) {
            // filter empty key and value
            Map<String, String> headers = new HashMap<>(attributes.size());
            String key;
            String value;
            for (Map.Entry<String, String> entry : attributes.entrySet()) {
                key = entry.getKey();
                key = key == null ? null : key.trim();
                value = entry.getValue();
                if (key != null && !key.isEmpty() && value != null && !value.isEmpty()) {
                    headers.put(key, value);
                }
            }
            attributes = headers;
        } else {
            attributes = new HashMap<>();
        }
    }
}
