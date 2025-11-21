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

import java.io.Serializable;

/**
 * Represents a JSON-RPC message.
 */
public interface JsonRpcMessage extends Serializable {

    String JSON_RPC_VERSION = "2.0";

    String METHOD_INITIALIZE = "initialize";
    String METHOD_NOTIFICATIONS_INITIALIZED = "notifications/initialized";
    String METHOD_PING = "ping";
    String METHOD_NOTIFICATIONS_PROGRESS = "notifications/progress";
    String METHOD_NOTIFICATIONS_CANCELLED = "notifications/cancelled";

    String METHOD_TOOLS_LIST = "tools/list";
    String METHOD_TOOLS_CALL = "tools/call";
    String METHOD_NOTIFICATION_TOOLS_LIST_CHANGED = "notifications/tools/list_changed";

    String METHOD_RESOURCES_LIST = "resources/list";
    String METHOD_RESOURCES_TEMPLATES_LIST = "resources/templates/list";
    String METHOD_RESOURCES_READ = "resources/read";
    String METHOD_RESOURCES_SUBSCRIBE = "resources/subscribe";
    String METHOD_NOTIFICATION_RESOURCES_LIST_CHANGED = "notifications/resources/list_changed";
    String METHOD_NOTIFICATION_RESOURCES_UPDATED = "notifications/resources/updated";

    String METHOD_LOGGING_SET_LEVEL = "logging/setLevel";
    String METHOD_NOTIFICATION_MESSAGE = "notifications/message";

    String METHOD_PROMPTS_LIST = "prompts/list";
    String METHOD_PROMPTS_GET = "prompts/get";
    String METHOD_NOTIFICATION_PROMPTS_LIST_CHANGED = "notifications/prompts/list_changed";
    String METHOD_COMPLETION_COMPLETE = "completion/complete";

    String METHOD_ROOTS_LIST = "roots/list";
    String METHOD_NOTIFICATION_ROOTS_LIST_CHANGED = "notifications/roots/list_changed";

    String METHOD_SAMPLING_CREATE_MESSAGE = "sampling/createMessage";
    String METHOD_ELICITATION_CREATE = "elicitation/create";

    /**
     * Returns the JSON-RPC protocol version string.
     *
     * @return the JSON-RPC protocol version (typically "2.0")
     */
    String getJsonrpc();

}
