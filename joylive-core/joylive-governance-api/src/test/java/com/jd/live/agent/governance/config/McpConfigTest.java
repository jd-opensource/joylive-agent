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
package com.jd.live.agent.governance.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class McpConfigTest {

    private McpConfig mcpConfig;

    @BeforeEach
    void setUp() {
        mcpConfig = new McpConfig();
    }

    @Test
    void testDefaultValues() {
        // Test default values
        Assertions.assertFalse(mcpConfig.isEnabled());
        Assertions.assertEquals("/mcp", mcpConfig.getPath());
    }

    @Test
    void testIsMcpWhenDisabled() {
        // When disabled, should always return false regardless of path
        mcpConfig.setEnabled(false);
        mcpConfig.setPath("/mcp");

        Assertions.assertFalse(mcpConfig.isMcp("/mcp"));
        Assertions.assertFalse(mcpConfig.isMcp("/mcp/test"));
        Assertions.assertFalse(mcpConfig.isMcp("/mcp/api/v1"));
        Assertions.assertFalse(mcpConfig.isMcp("/other"));
    }

    @Test
    void testIsMcpWhenEnabled() {
        mcpConfig.setEnabled(true);
        mcpConfig.setPath("/mcp");

        // Test exact match
        Assertions.assertTrue(mcpConfig.isMcp("/mcp"));

        // Test sub-paths
        Assertions.assertTrue(mcpConfig.isMcp("/mcp/test"));
        Assertions.assertTrue(mcpConfig.isMcp("/mcp/api/v1"));
        Assertions.assertTrue(mcpConfig.isMcp("/mcp/service/health"));

        // Test non-matching paths
        Assertions.assertFalse(mcpConfig.isMcp("/api"));
        Assertions.assertFalse(mcpConfig.isMcp("/health"));
        Assertions.assertFalse(mcpConfig.isMcp("/mcpx"));
        Assertions.assertFalse(mcpConfig.isMcp("/other/mcp"));
    }

    @Test
    void testIsMcpWithCustomPath() {
        mcpConfig.setEnabled(true);
        mcpConfig.setPath("/api/mcp");

        // Test exact match
        Assertions.assertTrue(mcpConfig.isMcp("/api/mcp"));

        // Test sub-paths
        Assertions.assertTrue(mcpConfig.isMcp("/api/mcp/test"));
        Assertions.assertTrue(mcpConfig.isMcp("/api/mcp/v1/service"));

        // Test non-matching paths
        Assertions.assertFalse(mcpConfig.isMcp("/api"));
        Assertions.assertFalse(mcpConfig.isMcp("/mcp"));
        Assertions.assertFalse(mcpConfig.isMcp("/api/other"));
    }

    @Test
    void testInitializeWithNullPath() {
        mcpConfig.setPath(null);
        mcpConfig.initialize();

        Assertions.assertEquals("/mcp", mcpConfig.getPath());
    }

    @Test
    void testInitializeWithTrailingSlash() {
        mcpConfig.setPath("/api/mcp/");
        mcpConfig.initialize();

        Assertions.assertEquals("/api/mcp", mcpConfig.getPath());
    }

    @Test
    void testInitializeWithoutLeadingSlashAndWithTrailingSlash() {
        mcpConfig.setPath("api/mcp/");
        mcpConfig.initialize();

        Assertions.assertEquals("/api/mcp", mcpConfig.getPath());
    }

    @Test
    void testCompleteWorkflow() {
        // Test a complete workflow: set properties, initialize, and test functionality
        mcpConfig.setEnabled(true);
        mcpConfig.setPath("custom/mcp/");
        mcpConfig.initialize();

        Assertions.assertTrue(mcpConfig.isEnabled());
        Assertions.assertEquals("/custom/mcp", mcpConfig.getPath());

        // Test isMcp functionality after initialization
        Assertions.assertTrue(mcpConfig.isMcp("/custom/mcp"));
        Assertions.assertTrue(mcpConfig.isMcp("/custom/mcp/test"));
        Assertions.assertFalse(mcpConfig.isMcp("/other"));
    }
}