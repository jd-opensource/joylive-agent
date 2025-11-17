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
import java.util.ArrayList;
import java.util.List;

/**
 * The server's preferences for model selection, requested of the client during
 * sampling.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ModelPreferences implements Serializable {
    /**
     * Optional hints to use for model selection. If multiple hints are
     * specified, the client MUST evaluate them in order (such that the first match is
     * taken). The client SHOULD prioritize these hints over the numeric priorities, but
     * MAY still use the priorities to select from ambiguous matches
     */
    private List<ModelHint> hints;
    /**
     * How much to prioritize cost when selecting a model. A value of
     * means cost is not important, while a value of 1 means cost is the most important
     * factor
     */
    private Double costPriority;
    /**
     * How much to prioritize sampling speed (latency) when selecting
     * a model. A value of 0 means speed is not important, while a value of 1 means speed
     * is the most important factor
     */
    private Double speedPriority;
    /**
     * How much to prioritize intelligence and capabilities
     * when selecting a model. A value of 0 means intelligence is not important, while a
     * value of 1 means intelligence is the most important factor
     */
    private Double intelligencePriority;

    public void addHint(String name) {
        if (name != null) {
            if (hints == null) {
                hints = new ArrayList<>();
            }
            hints.add(new ModelHint(name));
        }
    }
}
