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
package com.jd.live.agent.plugin.application.springboot.v2.mcp.param;

import com.jd.live.agent.governance.mcp.ParameterParser;
import com.jd.live.agent.plugin.application.springboot.v2.mcp.param.web.JakartaServletParameterFactory;
import com.jd.live.agent.plugin.application.springboot.v2.mcp.param.web.JavaxServletParameterFactory;

import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;

/**
 * A composite factory that delegates to multiple SystemParameterFactory implementations.
 * Tries each factory in sequence until a valid supplier is found.
 */
public class CompositeSystemParameterFactory implements SystemParameterFactory {

    public static final SystemParameterFactory INSTANCE = new CompositeSystemParameterFactory();

    private List<SystemParameterFactory> factories = Arrays.asList(new JavaxServletParameterFactory(),
            new JakartaServletParameterFactory());

    @Override
    public ParameterParser getParser(Parameter parameter) {
        for (SystemParameterFactory factory : factories) {
            ParameterParser parser = factory.getParser(parameter);
            if (parser != null) {
                return parser;
            }
        }
        return null;
    }
}
