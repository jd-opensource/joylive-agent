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

import com.jd.live.agent.core.util.cache.LazyObject;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static com.jd.live.agent.core.util.StringUtils.isEmpty;
import static com.jd.live.agent.core.util.type.ClassUtils.loadClass;

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
    private boolean enabled = true;

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

    @Getter
    private transient byte[] responseBytes;

    @Getter
    private transient boolean text;

    @Setter
    private transient Function<ClassLoader, Class<?>> contentClassFunc;

    private transient LazyObject<Class<?>> contentClassCache = new LazyObject<>(null);

    public DegradeConfig() {
    }

    @Builder
    public DegradeConfig(boolean enabled, int responseCode, String contentType, Map<String, String> attributes, String responseBody) {
        this.enabled = enabled;
        this.responseCode = responseCode;
        this.contentType = contentType;
        this.attributes = attributes;
        this.responseBody = responseBody;
    }

    public DegradeConfig(DegradeConfig config) {
        this.enabled = config.enabled;
        this.responseCode = config.responseCode;
        this.contentType = config.contentType;
        this.attributes = config.attributes == null ? null : new HashMap<>(config.attributes);
        this.responseBody = config.responseBody;
    }

    public String getContentType() {
        return contentType == null ? TYPE_APPLICATION_JSON : contentType;
    }

    public String getContentType(String defaultValue) {
        return contentType == null || contentType.isEmpty() ? defaultValue : contentType;
    }

    public String contentTypeOrDefault() {
        return getContentType(TYPE_APPLICATION_JSON);
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

    public int bodyLength() {
        return responseBody == null ? 0 : responseBody.length();
    }

    public Class<?> getContentClass(ClassLoader classLoader) {
        if (!isEmpty(contentType)) {
            // cache
            return contentClassCache.get(() -> loadClass(contentType, classLoader, false));
        }
        // not cache
        return contentClassFunc == null ? null : contentClassFunc.apply(classLoader);
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
        responseBytes = responseBody == null ? new byte[0] : responseBody.getBytes();
        text = TYPE_STRING.equalsIgnoreCase(contentType)
                || TYPE_JSON.equalsIgnoreCase(contentType)
                || TYPE_APPLICATION_TEXT.equalsIgnoreCase(contentType)
                || TYPE_APPLICATION_JSON.equalsIgnoreCase(contentType);
    }
}
