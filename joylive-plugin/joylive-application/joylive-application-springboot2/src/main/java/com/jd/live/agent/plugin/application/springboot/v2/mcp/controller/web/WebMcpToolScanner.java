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
package com.jd.live.agent.plugin.application.springboot.v2.mcp.controller.web;

import com.jd.live.agent.governance.mcp.McpToolScanner;
import com.jd.live.agent.plugin.application.springboot.v2.mcp.controller.AbstractMcpToolScanner;
import com.jd.live.agent.plugin.application.springboot.v2.mcp.param.SystemParameterFactory;
import com.jd.live.agent.plugin.application.springboot.v2.mcp.param.web.JakartaServletParameterFactory;
import com.jd.live.agent.plugin.application.springboot.v2.mcp.param.web.JavaxServletParameterFactory;
import com.jd.live.agent.plugin.application.springboot.v2.util.SpringUtils;

import java.lang.reflect.Parameter;

/**
 * Default scanner for MCP tools based on Spring MVC controllers.
 */
public class WebMcpToolScanner extends AbstractMcpToolScanner {

    public static final McpToolScanner INSTANCE = new WebMcpToolScanner();

    @Override
    protected SystemParameterFactory getSystemParameterFactory() {
        return SpringUtils.isJavaxServlet() ? new JavaxServletParameterFactory() : new JakartaServletParameterFactory();
    }

    @Override
    protected ParameterType getParameterType(Parameter parameter) {
        return new ParameterType(parameter.getType(), parameter.getParameterizedType(), null);
    }
}
