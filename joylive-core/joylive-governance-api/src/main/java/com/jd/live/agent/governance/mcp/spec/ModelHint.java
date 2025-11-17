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

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * The server's preferences for model selection, requested of the client during
 * sampling.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ModelHint implements Serializable {
    /**
     * A hint for a model name. The client SHOULD treat this as a substring of
     * a model name; for example: `claude-3-5-sonnet` should match
     * `claude-3-5-sonnet-20241022`, `sonnet` should match `claude-3-5-sonnet-20241022`,
     * `claude-3-sonnet-20240229`, etc., `claude` should match any Claude model. The
     * client MAY also map the string to a different provider's model name or a different
     * model family, as long as it fills a similar niche
     */
    private String name;

    public static ModelHint of(String name) {
        return new ModelHint(name);
    }
}
