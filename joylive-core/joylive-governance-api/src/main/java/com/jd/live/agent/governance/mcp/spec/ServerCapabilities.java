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
 * Capabilities that a server may support. Known capabilities are defined here, in
 * this schema, but this is not a closed set: any server can define its own,
 * additional capabilities.
 *
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServerCapabilities implements Serializable {

    /**
     * resent if the server supports argument autocompletion suggestions
     */
    private CompletionCapabilities completions;

    /**
     * Experimental, non-standard capabilities that the server supports
     */
    private Map<String, Object> experimental;

    /**
     * Present if the server supports sending log messages to the client
     */
    private LoggingCapabilities logging;

    /**
     * Present if the server offers any prompt templates
     */
    private PromptCapabilities prompts;

    /**
     * Present if the server offers any resources to read
     */
    private ResourceCapabilities resources;

    /**
     * Present if the server offers any tools to call
     */
    private ToolCapabilities tools;

    /**
     * Present if the server supports argument autocompletion suggestions.
     */
    public static class CompletionCapabilities implements Serializable {

    }

    /**
     * Present if the server supports sending log messages to the client.
     */
    public static class LoggingCapabilities implements Serializable {

    }

    /**
     * Present if the server offers any prompt templates.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PromptCapabilities implements Serializable {

        /**
         * Whether this server supports notifications for changes to
         */
        private boolean listChanged;

    }

    /**
     * Present if the server offers any resources to read.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResourceCapabilities implements Serializable {

        /**
         * Whether this server supports subscribing to resource updates
         */
        private boolean subscribe;

        /**
         * Whether this server supports notifications for changes to
         */
        private boolean listChanged;

    }

    /**
     * Present if the server offers any tools to call.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolCapabilities implements Serializable {

        /**
         * Whether this server supports notifications for changes to
         */
        private boolean listChanged;

    }
}
