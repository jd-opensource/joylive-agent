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
     * Present if the client supports task-augmented requests.
     */
    private Tasks tasks;

    /**
     * Present if the client supports listing roots.
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
     * Present if the client supports sampling from an LLM.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Sampling implements Serializable {

        /**
         * Whether the client supports context inclusion via includeContext parameter.
         * If not declared, servers SHOULD only use `includeContext: "none"` (or omit it).
         */
        private Object context;
        /**
         * Whether the client supports tool use via tools and toolChoice parameters.
         */
        private Object tools;

    }

    /**
     * Present if the client supports elicitation from the server.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Elicitation implements Serializable {

        private Object form;

        private Object url;

    }

    /**
     * Present if the client supports task-augmented requests.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Tasks implements Serializable {
        /**
         * Whether this client supports tasks/list.
         */
        private Object list;
        /**
         * Whether this client supports tasks/cancel.
         */
        private Object cancel;
        /**
         * Specifies which request types can be augmented with tasks.
         */
        private TaskRequests requests;
    }

    /**
     * Specifies which request types can be augmented with tasks.
     */
    public static class TaskRequests implements Serializable {
        /**
         * Task support for sampling-related requests.
         */
        private TaskSampling sampling;

        /**
         * Task support for elicitation-related requests.
         */
        private TaskElicitation elicitation;
    }

    /**
     * Task support for sampling-related requests.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskSampling implements Serializable {
        /**
         * Whether the client supports task-augmented sampling/createMessage requests.
         */
        private Object createMessage;
    }

    /**
     * Task support for elicitation-related requests.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskElicitation implements Serializable {
        /**
         * Whether the client supports task-augmented elicitation/create requests.
         */
        private Object create;
    }
}
