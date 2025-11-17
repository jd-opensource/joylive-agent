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
package com.jd.live.agent.governance.mcp.spec;

import com.jd.live.agent.core.parser.json.JsonField;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * The server's response to a completion/complete request.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CompleteResult implements Result {
    /**
     * The completion information containing values and metadata
     */
    private CompleteCompletion completion;
    /**
     * See specification for notes on _meta usage
     */
    @JsonField("_meta")
    private Map<String, Object> meta;

    public CompleteResult(CompleteCompletion completion) {
        this.completion = completion;
    }

    /**
     * The server's response to a completion/complete request
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompleteCompletion implements Serializable {
        /**
         * An array of completion values. Must not exceed 100 items
         */
        private List<String> values;
        /**
         * The total number of completion options available. This can exceed
         * the number of values actually sent in the response
         */
        private int total;
        /**
         * Indicates whether there are additional completion options beyond those provided in the current response,
         * even if the exact total is unknown
         */
        private boolean hasMore;
    }

}
