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
package com.jd.live.agent.core.mcp.converter;

import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.StreamableHttpMcpTransport;
import org.junit.jupiter.api.Test;

public class McpClientTest {

    @Test
    void testListTools() {
        String url = System.getenv("MCP_URL");
        url = url == null || url.isEmpty() ? "http://127.0.0.1:9999/mcp" : url;
        McpTransport transport = new StreamableHttpMcpTransport.Builder().url(url).build();
        McpClient mcpClient = new DefaultMcpClient.Builder().transport(transport).build();
        mcpClient.listTools().forEach(System.out::println);
    }
}
