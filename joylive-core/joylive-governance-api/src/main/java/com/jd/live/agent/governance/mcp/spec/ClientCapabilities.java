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

import lombok.*;

import java.io.Serializable;
import java.util.Map;

/**
 * Capabilities a client may support. Known capabilities are defined here, in this
 * schema, but this is not a closed set: any client can define its own, additional
 * capabilities.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientCapabilities implements Serializable {

    /**
     * Experimental, non-standard capabilities that the client supports
     */
    private Map<String, Object> experimental;

    /**
     * Present if the client supports listing roots
     */
    private RootCapabilities roots;

    /**
     * Present if the client supports sampling from an LLM
     */
    private Sampling sampling;

    /**
     * Present if the client supports elicitation from the server
     */
    private Elicitation elicitation;

    /**
     * Present if the client supports listing roots.
     *
     */
    @Getter
    @Setter
    public static class RootCapabilities implements Serializable {

        /**
         * Whether the client supports notifications for changes to the roots list
         */
        private boolean listChanged;

    }

    /**
     * Provides a standardized way for servers to request LLM sampling ("completions"
     * or "generations") from language models via clients. This flow allows clients to
     * maintain control over model access, selection, and permissions while enabling
     * servers to leverage AI capabilities—with no server API keys necessary. Servers
     * can request text or image-based interactions and optionally include context
     * from MCP servers in their prompts.
     */
    @Getter
    @Setter
    public static class Sampling implements Serializable {

    }

    /**
     * Provides a standardized way for servers to request additional information from
     * users through the client during interactions. This flow allows clients to
     * maintain control over user interactions and data sharing while enabling servers
     * to gather necessary information dynamically. Servers can request structured
     * data from users with optional JSON schemas to validate responses.
     */
    @Getter
    @Setter
    public static class Elicitation implements Serializable {

    }
}
