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
package com.jd.live.agent.core.mcp.spec.v1;

import com.jd.live.agent.core.parser.annotation.JsonField;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

/**
 * Binary contents of a resource.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BlobResourceContents implements ResourceContents {
    /**
     * the URI of this resource.
     */
    private String uri;
    /**
     * the MIME type of this resource.
     */
    private String mimeType;
    /**
     * the text of the resource. This must only be set if the resource can
     * actually be represented as text (not binary data).
     */
    private String text;
    /**
     * See specification for notes on _meta usage
     */
    @JsonField("_meta")
    private Map<String, Object> meta;

    public BlobResourceContents(String uri, String mimeType, String text) {
        this.uri = uri;
        this.mimeType = mimeType;
        this.text = text;
    }
}
