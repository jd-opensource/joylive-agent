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

import com.jd.live.agent.core.util.type.AnnotationGetter;
import com.jd.live.agent.core.util.type.AnnotationGetter.ParameterAnnotationGetter;
import com.jd.live.agent.core.util.type.AnnotationGetter.TypeAnnotationGetter;
import com.jd.live.agent.governance.mcp.*;
import com.jd.live.agent.governance.mcp.McpToolParameter.McpToolParameterBuilder;
import com.jd.live.agent.governance.mcp.McpToolParameterConfigurator.McpToolParameterConfiguratorChain;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static com.jd.live.agent.core.util.StringUtils.isEmpty;
import static com.jd.live.agent.core.util.StringUtils.url;

/**
 * Default implementation of McpToolScanner.
 * Scans Spring controllers and converts their methods to MCP tool methods.
 */
public abstract class AbstractMcpToolScanner implements McpToolScanner {

    protected final ExpressionFactory expressionFactory;

    public AbstractMcpToolScanner(ExpressionFactory expressionFactory) {
        this.expressionFactory = expressionFactory;
    }

    @Override
    public List<McpToolMethod> scan(Object controller) {
        // TODO handle ModelAttribute
        // model name by org.springframework.core.Conventions#getVariableNameForParameter
        List<McpToolMethod> tools = new ArrayList<>();
        Class<?> controllerClass = controller.getClass();
        String controllerName = getControllerName(controllerClass);
        String[] typePaths = getPaths(new TypeAnnotationGetter(controllerClass));
        for (Method method : controllerClass.getDeclaredMethods()) {
            if (filter(method)) {
                String[] methodPaths = getPaths(new AnnotationGetter.MethodAnnotationGetter(method));
                if (methodPaths != null) {
                    Set<String> fullPaths = appendPaths(typePaths, methodPaths);
                    String toolName = getToolName(controllerName, method, fullPaths);
                    tools.add(new McpToolMethod(toolName, controller, method, createParameters(method), fullPaths));
                }
            }
        }
        return tools;
    }

    /**
     * Filters which methods should be included as tools.
     * Default implementation includes all methods.
     *
     * @param method The method to check
     * @return true if method should be included, false otherwise
     */
    protected boolean filter(Method method) {
        return true;
    }

    /**
     * Generates a tool name by combining controller name and method name.
     *
     * @param controllerName name of the controller class
     * @param method         target method
     * @param fullPaths      set of API paths (unused)
     * @return combined tool name in format "controllerName.methodName"
     */
    protected String getToolName(String controllerName, Method method, Set<String> fullPaths) {
        // TODO Get name from strategy by full path
        String tool = controllerName + "." + method.getName();
        return tool;
    }

    /**
     * Combines root paths with sub paths to form complete URLs, filtering out empty paths.
     * For example: ["/api"] + ["/users", "", "/roles"] = ["/api/users", "/api/roles"]
     *
     * @param roots base paths, can be empty
     * @param paths sub paths to append
     * @return combined paths, or original paths if roots is empty
     */
    protected Set<String> appendPaths(String[] roots, String[] paths) {
        int rootLength = roots == null ? 0 : roots.length;
        if (rootLength == 0) {
            return appendPaths(paths);
        }
        int pathLength = paths == null ? 0 : paths.length;
        if (pathLength == 0) {
            return appendPaths(roots);
        }
        Set<String> results = new LinkedHashSet<>(rootLength * pathLength);
        for (String root : roots) {
            if (isEmpty(root)) continue;
            for (String path : paths) {
                if (isEmpty(path)) continue;
                results.add(url(root, path));
            }
        }
        return results;
    }

    /**
     * Filters empty paths and returns unique non-empty paths in a set.
     *
     * @param paths array of paths to filter
     * @return set of non-empty unique paths
     */
    protected Set<String> appendPaths(String[] paths) {
        if (paths == null) {
            return new LinkedHashSet<>();
        }
        Set<String> results = new LinkedHashSet<>(paths.length);
        for (String path : paths) {
            if (isEmpty(path)) continue;
            results.add(path);
        }
        return results;
    }

    /**
     * Gets request paths from Spring MVC mapping annotations (RequestMapping, GetMapping, etc).
     *
     * @param getter annotation getter function
     * @return mapped paths or null if no mapping found
     */
    protected String[] getPaths(AnnotationGetter getter) {
        RequestMapping requestMapping = getter.getAnnotation(RequestMapping.class);
        if (requestMapping != null) {
            return requestMapping.value().length > 0 ? requestMapping.value() : requestMapping.path();
        }
        GetMapping getMapping = getter.getAnnotation(GetMapping.class);
        if (getMapping != null) {
            return getMapping.value().length > 0 ? getMapping.value() : getMapping.path();
        }
        PostMapping postMapping = getter.getAnnotation(PostMapping.class);
        if (postMapping != null) {
            return postMapping.value().length > 0 ? postMapping.value() : postMapping.path();
        }
        PutMapping putMapping = getter.getAnnotation(PutMapping.class);
        if (putMapping != null) {
            return putMapping.value().length > 0 ? putMapping.value() : putMapping.path();
        }
        DeleteMapping deleteMapping = getter.getAnnotation(DeleteMapping.class);
        if (deleteMapping != null) {
            return deleteMapping.value().length > 0 ? deleteMapping.value() : deleteMapping.path();
        }
        PatchMapping patchMapping = getter.getAnnotation(PatchMapping.class);
        if (patchMapping != null) {
            return patchMapping.value().length > 0 ? patchMapping.value() : patchMapping.path();
        }
        return null;
    }

    /**
     * Converts controller class name to formatted name by removing "Controller" suffix
     * and converting first char to lowercase.
     *
     * @param type the controller class
     * @return formatted controller name
     */
    protected String getControllerName(Class<?> type) {
        String className = type.getSimpleName();
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
    protected McpToolParameter[] createParameters(Method method) {
        Parameter[] parameters = method.getParameters();
        if (parameters.length == 0) {
            return new McpToolParameter[0];
        }
        McpToolParameter[] result = new McpToolParameter[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            result[i] = createParameter(parameters[i], i);
        }
        return result;
    }

    /**
     * Builds MCP tool parameter from method parameter.
     *
     * @param parameter Method parameter to build from
     * @param index Parameter index in method
     * @return Built MCP tool parameter
     */
    protected McpToolParameter createParameter(Parameter parameter, int index) {
        McpToolParameterConfigurator chain = new McpToolParameterConfiguratorChain(
                builder -> configureRequestParam(builder, new ParameterAnnotationGetter(parameter)),
                builder -> configureType(builder),
                builder -> configureSystemParam(builder)
        );
        return chain.configure(McpToolParameter.builder().parameter(parameter).index(index).required(true)).build();
    }

    /**
     * Configures parameter name and requirements based on annotations.
     * Supports @RequestParam, @PathVariable and @RequestBody.
     *
     * @param builder parameter definition builder
     * @param getter  annotation accessor
     * @return configured builder
     */
    protected McpToolParameterBuilder configureRequestParam(McpToolParameterBuilder builder, AnnotationGetter getter) {
        RequestParam requestParam = getter.getAnnotation(RequestParam.class);
        if (requestParam != null) {
            return builder.arg(requestParam.value()).required(requestParam.required()).defaultValueParser(createDefaultValueParser(requestParam.defaultValue()));
        }
        PathVariable pathVariable = getter.getAnnotation(PathVariable.class);
        if (pathVariable != null) {
            return builder.arg(pathVariable.value()).required(pathVariable.required());
        }
        RequestBody requestBody = getter.getAnnotation(RequestBody.class);
        if (requestBody != null) {
            return builder.required(requestBody.required());
        }
        return builder;
    }

    /**
     * Configures parameter type information.
     *
     * @param builder the parameter builder to configure
     * @return builder with type information configured
     */
    protected abstract McpToolParameterBuilder configureType(McpToolParameterBuilder builder);

    /**
     * Configures system-level parameters.
     *
     * @param builder the parameter builder to configure
     * @return builder with system parameters configured
     */
    protected abstract McpToolParameterBuilder configureSystemParam(McpToolParameterBuilder builder);

    /**
     * Extracts actual type from generic type.
     *
     * @param genericType the generic type
     * @return actual type, or original type if not parameterized
     */
    protected Type getActualType(Type genericType) {
        return genericType == ParameterizedType.class
                ? ((ParameterizedType) genericType).getActualTypeArguments()[0]
                : null;
    }

    /**
     * Creates a parser for the default value if provided.
     *
     * @param defaultValue the default value to parse, may be null or empty
     * @return the request parser, or null if defaultValue is empty
     */
    protected McpRequestParser createDefaultValueParser(String defaultValue) {
        return createDefaultValueParser(defaultValue, null);
    }

    /**
     * Creates a parser for the default value with optional transformation.
     *
     * @param defaultValue the default value to parse, may be null or empty
     * @param function     optional transformation function to apply on parsed value
     * @return the request parser, or null if defaultValue equals DEFAULT_NONE
     */
    protected McpRequestParser createDefaultValueParser(String defaultValue, Function<Object, Object> function) {
        if (ValueConstants.DEFAULT_NONE.equals(defaultValue)) {
            return null;
        }
        Expression expression = expressionFactory.parse(defaultValue);
        if (function == null) {
            return ctx -> expressionFactory.evaluate(expression);
        } else {
            return ctx -> function.apply(expressionFactory.evaluate(expression));
        }
    }
}
