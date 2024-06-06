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
package com.jd.live.agent.governance.registry;

import lombok.*;

import java.io.Serializable;
import java.util.Map;

/**
 * Represents an instance of a service in a discovery system.
 */
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ServiceExport implements Serializable {

    @Getter
    private String schema;

    @Getter
    private String host;

    @Getter
    private int port;

    private String url;

    @Getter
    private Map<String, String> metadata;

    public String getUrl() {
        if (url == null) {
            String protocol = schema == null ? "" : schema;
            if (!protocol.isEmpty()) {
                protocol = protocol + "://";
            }
            url = protocol + host + ":" + port;
        }
        return url;
    }
}
