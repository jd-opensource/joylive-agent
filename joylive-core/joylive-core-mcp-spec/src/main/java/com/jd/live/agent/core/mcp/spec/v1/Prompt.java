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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A prompt or prompt template that the server offers.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Prompt implements Identifier {

    /**
     * The name of the prompt or prompt template.
     */
    private String name;
    /**
     * An optional title for the prompt.
     */
    private String title;
    /**
     * An optional description of what this prompt provides.
     */
    private String description;
    /**
     * A list of arguments to use for templating the prompt.
     */
    private List<PromptArgument> arguments;
    /**
     * See specification for notes on _meta usage
     */
    @JsonField("_meta")
    private Map<String, Object> meta;

    public Prompt(String name, String description, List<PromptArgument> arguments) {
        this(name, null, description, arguments != null ? arguments : new ArrayList<>());
    }

    public Prompt(String name, String title, String description, List<PromptArgument> arguments) {
        this(name, title, description, arguments != null ? arguments : new ArrayList<>(), null);
    }
}
