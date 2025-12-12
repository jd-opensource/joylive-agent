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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * Execution-related properties for a tool.
 *
 * @category `tools/list`
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ToolExecution implements Serializable {

    /**
     * Indicates whether this tool supports task-augmented execution.
     * This allows clients to handle long-running operations through polling
     * the task system.
     * Default: "forbidden"
     */
    private TaskSupport taskSupport;

    public enum TaskSupport {
        /**
         * Tool does not support task-augmented execution (default when absent)
         */
        @JsonField("forbidden")
        FORBIDDEN,
        /**
         * Tool may support task-augmented execution
         */
        @JsonField("optional")
        OPTIONAL,
        /**
         * Tool requires task-augmented execution
         */
        @JsonField("required")
        REQUIRED
    }
}
