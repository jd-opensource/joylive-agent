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

import com.jd.live.agent.core.mcp.spec.v1.Request.MetaRequest;
import com.jd.live.agent.core.parser.annotation.JsonField;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

/**
 * Sent from the client to request cancellation of resources/updated notifications
 * from the server. This should follow a previous resources/subscribe request.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UnsubscribeRequest implements MetaRequest {

    /**
     * the URI of the resource to subscribe to. The URI can use any protocol;
     * it is up to the server how to interpret it.
     */
    private String uri;
    /**
     * See specification for notes on _meta usage
     */
    @JsonField("_meta")
    private Map<String, Object> meta;

    public UnsubscribeRequest(String uri) {
        this(uri, null);
    }
}
