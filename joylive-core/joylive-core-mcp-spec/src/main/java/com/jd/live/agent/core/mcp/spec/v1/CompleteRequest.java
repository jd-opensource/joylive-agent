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
import java.util.Map;

/**
 * A request from the client to the server, to ask for completion options.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CompleteRequest implements Request.MetaRequest {
    /**
     * A reference to a prompt or resource template definition
     */
    private CompleteReference ref;
    /**
     * The argument's information for completion requests
     */
    private CompleteArgument argument;
    /**
     * Additional, optional context for completions
     */
    private CompleteContext context;
    /**
     * See specification for notes on _meta usage
     */
    @JsonField("_meta")
    private Map<String, Object> meta;

    public CompleteRequest(CompleteReference ref, CompleteArgument argument, Map<String, Object> meta) {
        this(ref, argument, null, meta);
    }

    public CompleteRequest(CompleteReference ref, CompleteArgument argument, CompleteContext context) {
        this(ref, argument, context, null);
    }

    public CompleteRequest(CompleteReference ref, CompleteArgument argument) {
        this(ref, argument, null, null);
    }

    /**
     * The argument's information for completion requests.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompleteArgument implements Serializable {
        /**
         * The name of the argument
         */
        private String name;
        /**
         * The value of the argument to use for completion matching
         */
        private String value;
    }

    /**
     * Additional, optional context for completions.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompleteContext implements Serializable {
        /**
         * Previously-resolved variables in a URI template or prompt
         */
        private Map<String, String> arguments;
    }
}
