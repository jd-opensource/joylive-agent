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
package com.jd.live.agent.core.mcp;

import com.jd.live.agent.core.mcp.McpToolParameter.McpToolParameterBuilder;

/**
 * Configures MCP tool parameters.
 */
public interface McpToolParameterConfigurator {

    /**
     * Configures parameter builder.
     *
     * @param builder the parameter builder to configure
     * @return configured builder
     */
    McpToolParameterBuilder configure(McpToolParameterBuilder builder);

    /**
     * Chains multiple parameter configurators together.
     */
    class McpToolParameterConfiguratorChain implements McpToolParameterConfigurator {

        private McpToolParameterConfigurator[] configurators;

        public McpToolParameterConfiguratorChain(McpToolParameterConfigurator... configurators) {
            this.configurators = configurators;
        }

        @Override
        public McpToolParameterBuilder configure(McpToolParameterBuilder builder) {
            McpToolParameterBuilder result = builder;
            if (configurators != null) {
                for (McpToolParameterConfigurator configurator : configurators) {
                    result = configurator.configure(result);
                }
            }
            return result;
        }
    }

}
