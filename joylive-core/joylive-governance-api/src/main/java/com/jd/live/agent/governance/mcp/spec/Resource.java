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
package com.jd.live.agent.governance.mcp.spec;

import com.jd.live.agent.core.parser.json.JsonField;
import lombok.*;

import java.util.Map;

/**
 * A known resource that the server is capable of reading.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Resource implements ResourceContent {

    /**
     * the URI of the resource.
     */
    private String uri;
    /**
     * A human-readable name for this resource. This can be used by clients to
     * populate UI elements.
     */
    private String name;
    /**
     * An optional title for this resource.
     */
    private String title;
    /**
     * A description of what this resource represents. This can be used
     * by clients to improve the LLM's understanding of available resources. It can be
     * thought of like a "hint" to the model.
     */
    private String description;
    /**
     * The MIME type of this resource, if known.
     */
    private String mimeType;
    /**
     * The size of the raw resource content, in bytes (i.e., before base64
     * encoding or any tokenization), if known. This can be used by Hosts to display file
     * sizes and estimate context window usage.
     */
    private Long size;
    /**
     * Optional annotations for the client. The client can use
     * annotations to inform how objects are used or displayed.
     */
    private Annotations annotations;
    /**
     * See specification for notes on _meta usage
     */
    @JsonField("_meta")
    private Map<String, Object> meta;

    public Resource(String uri, String name, String title, String description, String mimeType, Long size, Annotations annotations) {
        this(uri, name, title, description, mimeType, size, annotations, null);
    }

    public Resource(String uri, String name, String description, String mimeType, Long size, Annotations annotations) {
        this(uri, name, null, description, mimeType, size, annotations, null);
    }

    public Resource(String uri, String name, String description, String mimeType, Annotations annotations) {
        this(uri, name, null, description, mimeType, null, annotations, null);
    }
}
