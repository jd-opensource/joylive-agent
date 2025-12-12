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
import lombok.*;

import java.util.Map;

/**
 * The Model Context Protocol (MCP) provides a standardized way for servers to send
 * structured log messages to clients. Clients can control logging verbosity by
 * setting minimum log levels, with servers sending notifications containing severity
 * levels, optional logger names, and arbitrary JSON-serializable data.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoggingMessageNotification implements Notification.MetaNotification {
    /**
     * The severity levels. The minimum log level is set by the client.
     */
    private LoggingLevel level;
    /**
     * An optional name of the logger issuing this message.
     */
    private String logger;
    /**
     * The data to be logged, such as a string message or an object. Any JSON serializable type is allowed here.
     */
    private Object data;
    /**
     * See specification for notes on _meta usage
     */
    @JsonField("_meta")
    private Map<String, Object> meta;

    public LoggingMessageNotification(LoggingLevel level, String logger, String data) {
        this(level, logger, data, null);
    }
}
