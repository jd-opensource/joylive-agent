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

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * DegradeConfig
 *
 * @since 1.1.0
 */
@Setter
@Getter
public class DegradeConfig {

    private int responseCode = 200;

    private String contentType = "application/json";

    private Map<String, String> attributes;

    private String responseBody;

    public DegradeConfig() {
    }

    public DegradeConfig(DegradeConfig config) {
        this.responseCode = config.responseCode;
        this.contentType = config.contentType;
        this.attributes = config.attributes == null ? null : new HashMap<>(config.attributes);
        this.responseBody = config.responseBody;
    }

    public void foreach(BiConsumer<String, String> consumer) {
        if (attributes != null) {
            attributes.forEach(consumer);
        }
    }

    public int bodyLength() {
        return responseBody == null ? 0 : responseBody.length();
    }

}
