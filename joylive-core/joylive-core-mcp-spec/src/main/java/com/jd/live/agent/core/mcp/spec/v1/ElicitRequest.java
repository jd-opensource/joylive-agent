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

import java.util.Map;

/**
 * A request from the server to elicit additional information from the user via the
 * client.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ElicitRequest implements MetaRequest {

    private ElicitMode mode;

    /**
     * The message to present to the user
     */
    private String message;
    /**
     * A restricted subset of JSON Schema. Only top-level
     * properties are allowed, without nesting
     */
    private JsonSchema requestedSchema;
    /**
     * The ID of the elicitation, which must be unique within the context of the server.
     * The client MUST treat this ID as an opaque value.
     */
    private String elicitationId;
    /**
     * The URL that the user should navigate to.
     *
     * @format uri
     */
    private String url;
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

    public enum ElicitMode {
        @JsonField("form")
        FORM,
        @JsonField("url")
        URL

    }
}
