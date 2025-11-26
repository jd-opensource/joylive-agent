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

import com.jd.live.agent.core.mcp.spec.v1.Request.MetaRequest;
import com.jd.live.agent.core.parser.annotation.JsonField;
import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * A request from the server to sample an LLM via the client. The client has full
 * discretion over which model to select. The client should also inform the user
 * before beginning sampling, to allow them to inspect the request (human in the loop)
 * and decide whether to approve it.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateMessageRequest implements MetaRequest {
    /**
     * The conversation messages to send to the LLM
     */
    private List<SamplingMessage> messages;
    /**
     * The server's preferences for which model to select. The
     * client MAY ignore these preferences
     */
    private ModelPreferences modelPreferences;
    /**
     * An optional system prompt the server wants to use for sampling.
     * The client MAY modify or omit this prompt
     */
    private String systemPrompt;
    /**
     * A request to include context from one or more MCP servers (including the caller),
     * to be attached to the prompt. The client MAY ignore this request
     */
    private ContextInclusionStrategy includeContext;
    /**
     * Optional temperature parameter for sampling
     */
    private Double temperature;
    /**
     * The maximum number of tokens to sample, as requested by the server.
     * The client MAY choose to sample fewer tokens than requested
     */
    private Integer maxTokens;
    /**
     * Optional stop sequences for sampling
     */
    private List<String> stopSequences;
    /**
     * Optional metadata to pass through to the LLM provider. The format of this metadata is provider-specific.
     */
    private Object metadata;
    /**
     * Tools that the model may use during generation.
     * The client MUST return an error if this field is provided but ClientCapabilities.sampling.tools is not declared.
     */
    private List<Tool> tools;
    /**
     * Controls how the model uses tools.
     * The client MUST return an error if this field is provided but ClientCapabilities.sampling.tools is not declared.
     * Default is `{ mode: "auto" }`.
     */
    private ToolChoice toolChoice;
    /**
     * If specified, the caller is requesting task-augmented execution for this request.
     * The request will return a CreateTaskResult immediately, and the actual result can be
     * retrieved later via tasks/result.
     * <p>
     * Task augmentation is subject to capability negotiation - receivers MUST declare support
     * for task augmentation of specific request types in their capabilities.
     */
    private TaskMetadata task;
    /**
     * See specification for notes on _meta usage
     */
    @JsonField("_meta")
    private Map<String, Object> meta;

    public enum ContextInclusionStrategy {

        @JsonField("none")
        NONE,

        @JsonField("thisServer")
        THIS_SERVER,

        @JsonField("allServers")
        ALL_SERVERS
    }

    public enum ToolChoice {

        @JsonField("auto")
        AUTO,

        @JsonField("required")
        REQUIRED,

        @JsonField("none")
        NONE
    }
}
