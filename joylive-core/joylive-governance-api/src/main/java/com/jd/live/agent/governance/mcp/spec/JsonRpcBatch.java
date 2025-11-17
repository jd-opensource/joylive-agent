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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * JSON-RPC 2.0 Batch Support
 * <p>
 * To send several Request objects at the same time, the Client MAY send an Array filled with Request objects.
 * The Server should respond with an Array containing the corresponding Response objects.
 */
public class JsonRpcBatch implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<JsonRpcRequest> requests;
    private List<JsonRpcResponse> responses;

    public JsonRpcBatch() {
        this.requests = new ArrayList<>();
        this.responses = new ArrayList<>();
    }

    public JsonRpcBatch(List<JsonRpcRequest> requests) {
        this.requests = requests != null ? requests : new ArrayList<>();
        this.responses = new ArrayList<>();
    }

    public List<JsonRpcRequest> getRequests() {
        return requests;
    }

    public void setRequests(List<JsonRpcRequest> requests) {
        this.requests = requests;
    }

    public List<JsonRpcResponse> getResponses() {
        return responses;
    }

    public void setResponses(List<JsonRpcResponse> responses) {
        this.responses = responses;
    }

    /**
     * Add a request to the batch
     */
    public void addRequest(JsonRpcRequest request) {
        if (this.requests == null) {
            this.requests = new ArrayList<>();
        }
        this.requests.add(request);
    }

    /**
     * Add a response to the batch
     */
    public void addResponse(JsonRpcResponse response) {
        if (this.responses == null) {
            this.responses = new ArrayList<>();
        }
        this.responses.add(response);
    }

    /**
     * Check if this batch has any requests
     */
    public boolean hasRequests() {
        return requests != null && !requests.isEmpty();
    }

    /**
     * Check if this batch has any responses
     */
    public boolean hasResponses() {
        return responses != null && !responses.isEmpty();
    }

    /**
     * Get the number of requests in this batch
     */
    public int getRequestCount() {
        return requests != null ? requests.size() : 0;
    }

    /**
     * Get the number of responses in this batch
     */
    public int getResponseCount() {
        return responses != null ? responses.size() : 0;
    }

    /**
     * Check if this is an empty batch
     */
    public boolean isEmpty() {
        return !hasRequests() && !hasResponses();
    }

    /**
     * Clear all requests and responses
     */
    public void clear() {
        if (requests != null) {
            requests.clear();
        }
        if (responses != null) {
            responses.clear();
        }
    }

    @Override
    public String toString() {
        return "JsonRpcBatch{" +
                "requests=" + (requests != null ? requests.size() + " items" : "null") +
                ", responses=" + (responses != null ? responses.size() + " items" : "null") +
                '}';
    }
}