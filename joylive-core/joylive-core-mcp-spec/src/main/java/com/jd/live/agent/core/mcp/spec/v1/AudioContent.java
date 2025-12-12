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
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * Audio provided to or from an LLM.
 */
public class AudioContent implements Annotated, Content {

    private String type = TYPE_AUDIO;

    /**
     * The base64-encoded audio data
     */
    @Getter
    @Setter
    private String data;
    /**
     * The MIME type of the audio. Different providers may support different audio types
     */
    @Getter
    @Setter
    private String mimeType;
    /**
     * Optional annotations for the client
     */
    @Getter
    @Setter
    private Annotations annotations;
    /**
     * See specification for notes on _meta usage
     */
    @Getter
    @Setter
    @JsonField("_meta")
    private Map<String, Object> meta;

    public AudioContent() {
    }

    public AudioContent(String data, String mimeType) {
        this.data = data;
        this.mimeType = mimeType;
    }

    public AudioContent(String data, String mimeType, Annotations annotations, Map<String, Object> meta) {
        this.data = data;
        this.mimeType = mimeType;
        this.annotations = annotations;
        this.meta = meta;
    }

    @Override
    public String getType() {
        return type;
    }

    public void setType(String type) {

    }
}
