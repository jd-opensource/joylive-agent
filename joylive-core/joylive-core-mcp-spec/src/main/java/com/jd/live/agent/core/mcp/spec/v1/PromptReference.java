/*
 * Copyright © ${year} ${owner} (${email})
 *
 * Licensed under the Apache License; Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing; software
 * distributed under the License is distributed on an "AS IS" BASIS;
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND; either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jd.live.agent.core.mcp.spec.v1;

import com.jd.live.agent.core.parser.annotation.JsonField;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * Identifies a prompt for completion requests.
 */
public class PromptReference implements CompleteReference, Identifier, Meta {

    /**
     * The reference type identifier (typically "ref/prompt")
     */
    private String type = TYPE_REF_PROMPT;

    /**
     * The name of the prompt
     */
    @Getter
    @Setter
    private String name;
    /**
     * An optional title for the prompt
     */
    @Getter
    @Setter
    private String title;

    /**
     * See specification for notes on _meta usage
     */
    @Getter
    @Setter
    @JsonField("_meta")
    private Map<String, Object> meta;

    public PromptReference() {
    }

    public PromptReference(String name, String title) {
        this(name, title, null);
    }

    public PromptReference(String name, String title, Map<String, Object> meta) {
        this.name = name;
        this.title = title;
        this.meta = meta;
    }

    public String getType() {
        return TYPE_REF_PROMPT;
    }

    public void setType(String type) {

    }

    @Override
    public String identifier() {
        return getName();
    }
}
