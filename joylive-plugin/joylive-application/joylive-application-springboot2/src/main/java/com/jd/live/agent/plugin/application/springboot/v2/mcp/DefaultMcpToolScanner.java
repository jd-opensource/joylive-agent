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
package com.jd.live.agent.plugin.application.springboot.v2.mcp;

import com.jd.live.agent.governance.mcp.McpToolMethod;
import com.jd.live.agent.governance.mcp.McpToolParameter;
import com.jd.live.agent.governance.mcp.McpToolScanner;
import com.jd.live.agent.plugin.application.springboot.v2.util.SpringUtils;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.jd.live.agent.core.util.StringUtils.isEmpty;

/**
 * Default implementation of McpToolScanner.
 * Scans Spring MVC controllers and converts their methods to MCP tool methods.
 */
public class DefaultMcpToolScanner implements McpToolScanner {

    public static final McpToolScanner INSTANCE = new DefaultMcpToolScanner();

    private final List<Function<Parameter, ParameterName>> functions = new ArrayList<>();

    public DefaultMcpToolScanner() {
        functions.add(this::getRequestParam);
        functions.add(this::getPathVariable);
        functions.add(this::getRequestHeader);
        functions.add(this::getRequestBody);
    }

    @Override
    public List<McpToolMethod> scan(Object controller) {
        List<McpToolMethod> tools = new ArrayList<>();
        String controllerName = getControllerName(controller);
        for (Method method : controller.getClass().getMethods()) {
            if (isRequestMapping(method)) {
                String tool = controllerName + "." + method.getName();
                tools.add(new McpToolMethod(tool, controller, method, getParameters(method)));
            }
        }
        return tools;
    }

    /**
     * Checks if method has Spring MVC request mapping annotations.
     *
     * @param method Method to check
     * @return true if method has request mapping annotations
     */
    private boolean isRequestMapping(Method method) {
        return method.isAnnotationPresent(RequestMapping.class) ||
                method.isAnnotationPresent(GetMapping.class) ||
                method.isAnnotationPresent(PostMapping.class) ||
                method.isAnnotationPresent(PutMapping.class) ||
                method.isAnnotationPresent(DeleteMapping.class) ||
                method.isAnnotationPresent(PatchMapping.class);
    }

    /**
     * Gets controller name by converting class name.
     *
     * @param controller Controller instance
     * @return Formatted controller name
     */
    private String getControllerName(Object controller) {
        String className = controller.getClass().getSimpleName();
        String prefix = className.replace("Controller", "");
        prefix = prefix.substring(0, 1).toLowerCase() + prefix.substring(1);
        return prefix;
    }

    /**
     * Converts method parameters to MCP tool parameters.
     *
     * @param method Method to get parameters from
     * @return Array of MCP tool parameters
     */
    private McpToolParameter[] getParameters(Method method) {
        Parameter[] parameters = method.getParameters();
        if (parameters.length == 0) {
            return new McpToolParameter[0];
        }
        McpToolParameter[] result = new McpToolParameter[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            result[i] = build(parameters[i], i);
        }
        return result;
    }

    /**
     * Builds MCP tool parameter from method parameter.
     *
     * @param parameter Method parameter
     * @param index     Parameter index
     * @return MCP tool parameter
     */
    private McpToolParameter build(Parameter parameter, int index) {
        ParameterName parameterName = null;
        for (Function<Parameter, ParameterName> function : functions) {
            parameterName = function.apply(parameter);
            if (parameterName != null) {
                break;
            }
        }
        boolean required = parameterName != null && parameterName.isRequired();
        String name = parameterName != null && !isEmpty(parameterName.getName()) ? parameterName.getName() : parameter.getName();
        Class<?> type = parameter.getType();
        Type genericType = parameter.getParameterizedType();
        Function<Object, Object> mono = SpringUtils.getMonoConverter(type);
        genericType = mono != null && genericType instanceof ParameterizedType
                ? ((ParameterizedType) genericType).getActualTypeArguments()[0]
                : genericType;
        Supplier<Object> supplier = required || mono != null ? null : SpringUtils.getSystemSupplier(parameter);
        return new McpToolParameter(name, index, type, genericType, required, mono, supplier);
    }

    /**
     * Extracts RequestParam annotation information from parameter
     *
     * @param parameter Method parameter
     * @return Parameter name and required flag, or null if no annotation
     */
    private ParameterName getRequestParam(Parameter parameter) {
        RequestParam requestParam = parameter.getAnnotation(RequestParam.class);
        if (requestParam != null) {
            return new ParameterName(requestParam.name(), requestParam.required());
        }
        return null;
    }

    /**
     * Extracts PathVariable annotation information from parameter
     *
     * @param parameter Method parameter
     * @return Parameter name and required flag, or null if no annotation
     */
    private ParameterName getPathVariable(Parameter parameter) {
        PathVariable pathVariable = parameter.getAnnotation(PathVariable.class);
        if (pathVariable != null) {
            return new ParameterName(pathVariable.name(), pathVariable.required());
        }
        return null;
    }

    /**
     * Extracts RequestHeader annotation information from parameter
     *
     * @param parameter Method parameter
     * @return Parameter name and required flag, or null if no annotation
     */
    private ParameterName getRequestHeader(Parameter parameter) {
        RequestHeader requestHeader = parameter.getAnnotation(RequestHeader.class);
        if (requestHeader != null) {
            return new ParameterName(requestHeader.name(), requestHeader.required());
        }
        return null;
    }

    /**
     * Extracts RequestBody annotation information from parameter
     *
     * @param parameter Method parameter
     * @return Required flag with null name, or null if no annotation
     */
    private ParameterName getRequestBody(Parameter parameter) {
        RequestBody requestBody = parameter.getAnnotation(RequestBody.class);
        if (requestBody != null) {
            return new ParameterName(null, requestBody.required());
        }
        return null;
    }

    /**
     * Holds parameter name and required flag
     */
    private static class ParameterName {

        private String name;

        private boolean required;

        ParameterName(String name, boolean required) {
            this.name = name;
            this.required = required;
        }

        public String getName() {
            return name;
        }

        public boolean isRequired() {
            return required;
        }
    }

}
