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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.jd.live.agent.core.util.StringUtils.isEmpty;
import static com.jd.live.agent.core.util.StringUtils.url;

/**
 * Default implementation of McpToolScanner.
 * Scans Spring MVC controllers and converts their methods to MCP tool methods.
 */
public class DefaultMcpToolScanner implements McpToolScanner {

    public static final McpToolScanner INSTANCE = new DefaultMcpToolScanner();

    @Override
    public List<McpToolMethod> scan(Object controller) {
        List<McpToolMethod> tools = new ArrayList<>();
        Class<?> controllerClass = controller.getClass();
        String controllerName = getControllerName(controllerClass);
        String[] typePaths = getPaths(new TypeAnnotationGetter(controllerClass));
        for (Method method : controllerClass.getMethods()) {
            String[] methodPaths = getPaths(new MethodAnnotationGetter(method));
            if (methodPaths != null) {
                Set<String> fullPaths = appendPaths(typePaths, methodPaths);
                String toolName = getToolName(controllerName, method, fullPaths);
                tools.add(new McpToolMethod(toolName, controller, method, getParameters(method), fullPaths));
            }
        }
        return tools;
    }

    /**
     * Generates a tool name by combining controller name and method name.
     *
     * @param controllerName name of the controller class
     * @param method         target method
     * @param fullPaths      set of API paths (unused)
     * @return combined tool name in format "controllerName.methodName"
     */
    private String getToolName(String controllerName, Method method, Set<String> fullPaths) {
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
    private Set<String> appendPaths(String[] roots, String[] paths) {
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
    private Set<String> appendPaths(String[] paths) {
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
    private String[] getPaths(AnnotationGetter getter) {
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
     * Extracts parameter information from Spring annotations (RequestParam, PathVariable, RequestHeader, RequestBody).
     *
     * @param getter annotation getter to retrieve annotations
     * @return parameter name and required flag, or null if no supported annotation found
     */
    private ParameterName getParam(AnnotationGetter getter) {
        RequestParam requestParam = getter.getAnnotation(RequestParam.class);
        if (requestParam != null) {
            return new ParameterName(!requestParam.value().isEmpty() ? requestParam.value() : requestParam.name(), requestParam.required());
        }
        PathVariable pathVariable = getter.getAnnotation(PathVariable.class);
        if (pathVariable != null) {
            return new ParameterName(!pathVariable.value().isEmpty() ? pathVariable.value() : pathVariable.name(), pathVariable.required());
        }
        RequestHeader requestHeader = getter.getAnnotation(RequestHeader.class);
        if (requestHeader != null) {
            return new ParameterName(!requestHeader.value().isEmpty() ? requestHeader.value() : requestHeader.name(), requestHeader.required());
        }
        RequestBody requestBody = getter.getAnnotation(RequestBody.class);
        if (requestBody != null) {
            return new ParameterName(null, requestBody.required());
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
    private String getControllerName(Class<?> type) {
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
        ParameterName parameterName = getParam(new ParameterAnnotationGetter(parameter));
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
     * Holds parameter name and required flag
     */
    private static class ParameterName {

        private final String name;

        private final boolean required;

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

    /**
     * Functional interface for retrieving annotations from different sources.
     */
    @FunctionalInterface
    interface AnnotationGetter {
        /**
         * Gets annotation of specified type from the source.
         *
         * @param annotationClass the Class object of the annotation type
         * @return the annotation if present, else null
         */
        <A extends Annotation> A getAnnotation(Class<A> annotationClass);
    }

    /**
     * Implementation for getting annotations from a Class type.
     */
    static class TypeAnnotationGetter implements AnnotationGetter {
        private final Class<?> type;

        TypeAnnotationGetter(Class<?> type) {
            this.type = type;
        }

        @Override
        public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
            return type.getAnnotation(annotationClass);
        }
    }

    /**
     * Implementation for getting annotations from a Method.
     */
    static class MethodAnnotationGetter implements AnnotationGetter {
        private final Method method;

        MethodAnnotationGetter(Method method) {
            this.method = method;
        }

        @Override
        public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
            return method.getAnnotation(annotationClass);
        }
    }

    /**
     * Implementation for getting annotations from a Parameter.
     */
    static class ParameterAnnotationGetter implements AnnotationGetter {

        private final Parameter parameter;

        ParameterAnnotationGetter(Parameter parameter) {
            this.parameter = parameter;
        }

        @Override
        public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
            return parameter.getAnnotation(annotationClass);
        }
    }

}
