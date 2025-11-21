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

import java.util.List;
import java.util.Map;

/**
 * An image provided to or from an LLM.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ImageContent implements Annotated, Content {

    /**
     * Optional annotations for the client
     */
    private Annotations annotations;
    /**
     * The base64-encoded image data
     */
    private String data;
    /**
     * The MIME type of the image. Different providers may support different image types
     */
    private String mimeType;
    /**
     * See specification for notes on _meta usage
     */
    @JsonField("_meta")
    private Map<String, Object> meta;

    public ImageContent(Annotations annotations, String data, String mimeType) {
        this(annotations, data, mimeType, null);
    }

    public ImageContent(List<Role> audience, Double priority, String data, String mimeType) {
        this(audience != null || priority != null ? new Annotations(audience, priority) : null, data, mimeType,
                null);
    }

    @Override
    public String getType() {
        return "image";
    }

    public List<Role> audience() {
        return annotations == null ? null : annotations.getAudience();
    }

    @Deprecated
    public Double priority() {
        return annotations == null ? null : annotations.getPriority();
    }
}
