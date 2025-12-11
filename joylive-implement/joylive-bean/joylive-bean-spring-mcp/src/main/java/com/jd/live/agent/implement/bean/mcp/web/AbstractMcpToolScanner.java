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
package com.jd.live.agent.implement.bean.mcp.web;

import com.jd.live.agent.core.mcp.*;
import com.jd.live.agent.core.mcp.McpToolParameter.Location;
import com.jd.live.agent.core.mcp.McpToolParameter.McpToolParameterBuilder;
import com.jd.live.agent.core.mcp.McpToolParameterConfigurator.McpToolParameterConfiguratorChain;
import com.jd.live.agent.core.util.map.MultiMap;
import com.jd.live.agent.core.util.type.AnnotationGetter;
import com.jd.live.agent.core.util.type.AnnotationGetter.MethodAnnotationGetter;
import com.jd.live.agent.core.util.type.AnnotationGetter.ParameterAnnotationGetter;
import com.jd.live.agent.core.util.type.AnnotationGetter.TypeAnnotationGetter;
import com.jd.live.agent.implement.bean.mcp.expression.SpringExpressionFactory;
import com.jd.live.agent.implement.bean.mcp.util.SpringUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Function;

import static com.jd.live.agent.core.util.CollectionUtils.cascadeAndGet;
import static com.jd.live.agent.core.util.StringUtils.*;

/**
 * Default implementation of McpToolScanner.
 * Scans Spring controllers and converts their methods to MCP tool methods.
 */
public abstract class AbstractMcpToolScanner implements McpToolScanner {

    /**
     * Spring bean factory for accessing application context
     */
    protected final ConfigurableBeanFactory beanFactory;

    /**
     * Factory for creating and evaluating expressions
     */
    protected final ExpressionFactory expressionFactory;

    /**
     * Discovers parameter names from compiled code
     */
    protected final ParameterNameDiscoverer nameDiscoverer;

    /**
     * Creates a new scanner with the specified bean factory
     *
     * @param beanFactory Spring bean factory for accessing application context
     */
    public AbstractMcpToolScanner(ConfigurableListableBeanFactory beanFactory) {
        this.beanFactory = beanFactory;
        this.expressionFactory = new SpringExpressionFactory(beanFactory);
        this.nameDiscoverer = SpringUtils.getParameterNameDiscoverer(beanFactory);
    }

    /**
     * Scans a controller object and converts its methods to MCP tool methods
     *
     * @param controller The controller object to scan
     * @return List of MCP tool methods found in the controller
     */
    @Override
    public List<McpToolMethod> scan(Object controller) {
        // model name by org.springframework.core.Conventions#getVariableNameForParameter
        List<McpToolMethod> tools = new ArrayList<>();
        Class<?> controllerClass = controller.getClass();
        String controllerName = getControllerName(controllerClass);
        PathMethod typePath = getPathMethod(new TypeAnnotationGetter(controllerClass));
        for (Method method : controllerClass.getDeclaredMethods()) {
            if (filter(method)) {
                PathMethod methodPath = getPathMethod(new MethodAnnotationGetter(method));
                if (methodPath == null && typePath == null) {
                    continue;
                }
                Set<String> fullPaths = methodPath == null ? typePath.mergePaths(null) : methodPath.mergePaths(typePath);
                Set<String> methods = method == null ? typePath.mergeMethods(null) : methodPath.mergeMethods(typePath);
                String toolName = getToolName(controllerName, method, fullPaths);
                tools.add(new McpToolMethod(toolName, controller, method, createParameters(method), fullPaths, methods));
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
     * Gets request paths from Spring MVC mapping annotations (RequestMapping, GetMapping, etc).
     *
     * @param getter annotation getter function
     * @return mapped paths or null if no mapping found
     */
    protected PathMethod getPathMethod(AnnotationGetter getter) {
        RequestMapping requestMapping = getter.getAnnotation(RequestMapping.class);
        if (requestMapping != null) {
            return requestMapping.value().length > 0
                    ? new PathMethod(requestMapping.value(), requestMapping.method())
                    : new PathMethod(requestMapping.path(), requestMapping.method());
        }
        GetMapping getMapping = getter.getAnnotation(GetMapping.class);
        if (getMapping != null) {
            return getMapping.value().length > 0
                    ? new PathMethod(getMapping.value(), new RequestMethod[]{RequestMethod.GET})
                    : new PathMethod(getMapping.path(), new RequestMethod[]{RequestMethod.GET});
        }
        PostMapping postMapping = getter.getAnnotation(PostMapping.class);
        if (postMapping != null) {
            return postMapping.value().length > 0
                    ? new PathMethod(postMapping.value(), new RequestMethod[]{RequestMethod.POST})
                    : new PathMethod(postMapping.path(), new RequestMethod[]{RequestMethod.POST});
        }
        PutMapping putMapping = getter.getAnnotation(PutMapping.class);
        if (putMapping != null) {
            return putMapping.value().length > 0
                    ? new PathMethod(putMapping.value(), new RequestMethod[]{RequestMethod.PUT})
                    : new PathMethod(putMapping.path(), new RequestMethod[]{RequestMethod.PUT});
        }
        DeleteMapping deleteMapping = getter.getAnnotation(DeleteMapping.class);
        if (deleteMapping != null) {
            return deleteMapping.value().length > 0
                    ? new PathMethod(deleteMapping.value(), new RequestMethod[]{RequestMethod.DELETE})
                    : new PathMethod(deleteMapping.path(), new RequestMethod[]{RequestMethod.DELETE});
        }
        PatchMapping patchMapping = getter.getAnnotation(PatchMapping.class);
        if (patchMapping != null) {
            return patchMapping.value().length > 0
                    ? new PathMethod(patchMapping.value(), new RequestMethod[]{RequestMethod.PATCH})
                    : new PathMethod(patchMapping.path(), new RequestMethod[]{RequestMethod.PATCH});
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
        String[] names = nameDiscoverer == null ? null : nameDiscoverer.getParameterNames(method);
        Parameter[] parameters = method.getParameters();
        if (parameters.length == 0) {
            return new McpToolParameter[0];
        }
        McpToolParameter[] result = new McpToolParameter[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            String name = names == null ? parameter.getName() : names[i];
            result[i] = createParameter(method, parameter, i, name);
        }
        return result;
    }

    /**
     * Builds MCP tool parameter from method parameter.
     *
     * @param method    Method to get parameters from
     * @param parameter Method parameter to build from
     * @param index     Parameter index in method
     * @param name      Parameter name
     * @return Built MCP tool parameter
     */
    protected McpToolParameter createParameter(Method method, Parameter parameter, int index, String name) {
        // TODO parse complex model object by system json parser.
        McpToolParameterConfigurator chain = new McpToolParameterConfiguratorChain(
                builder -> configureRequestParam(builder, new ParameterAnnotationGetter(parameter)),
                builder -> configureModelAttribute(builder, new ParameterAnnotationGetter(parameter), new MethodAnnotationGetter(method)),
                builder -> configurePathVariable(builder, new ParameterAnnotationGetter(parameter)),
                builder -> configureRequestHeader(builder, new ParameterAnnotationGetter(parameter)),
                builder -> configureCookieValue(builder, new ParameterAnnotationGetter(parameter)),
                builder -> configureRequestBody(builder, new ParameterAnnotationGetter(parameter)),
                builder -> configureRequestAttribute(builder, new ParameterAnnotationGetter(parameter)),
                builder -> configureSessionAttribute(builder, new ParameterAnnotationGetter(parameter)),
                builder -> configureSystemParam(builder),
                builder -> configureDefault(builder),
                builder -> configureWrapper(builder),
                builder -> configureValidator(builder)
        );
        return chain.configure(McpToolParameter.builder().parameter(parameter).name(name).index(index).required(true)).build();
    }

    /**
     * Configures @RequestParam based on annotations.
     *
     * @param builder parameter definition builder
     * @param getter  annotation accessor
     * @return configured builder
     */
    protected McpToolParameterBuilder configureRequestParam(McpToolParameterBuilder builder, AnnotationGetter getter) {
        RequestParam requestParam = getter.getAnnotation(RequestParam.class);
        if (requestParam != null) {
            // In Spring MVC, "request parameters" map to query parameters, form data,
            // and parts in multipart requests. This is because the Servlet API combines
            // query parameters and form data into a single map called "parameters", and
            // that includes automatic parsing of the request body.
            String arg = resolveName(choose(requestParam.value(), requestParam.name()));
            return builder
                    .arg(arg)
                    .required(requestParam.required())
                    .location(Location.QUERY)
                    .defaultValueParser(createDefaultValueParser(requestParam.defaultValue()));
        }
        return builder;
    }

    /**
     * Configures @ModelAttribute based on annotations.
     *
     * @param builder         parameter definition builder
     * @param parameterGetter parameter annotation accessor
     * @param methodGetter    method annotation accessor
     * @return configured builder
     */
    protected McpToolParameterBuilder configureModelAttribute(McpToolParameterBuilder builder,
                                                              AnnotationGetter parameterGetter,
                                                              AnnotationGetter methodGetter) {
        ModelAttribute modelAttribute = parameterGetter.getAnnotation(ModelAttribute.class);
        if (modelAttribute != null) {
            Location location = Location.QUERY;
            String arg = resolveName(choose(modelAttribute.value(), modelAttribute.name()));
            if (methodGetter.getAnnotation(PostMapping.class) != null
                    || methodGetter.getAnnotation(PutMapping.class) != null) {
                location = Location.BODY;
            }
            return configureModelAttribute(builder, arg, location);
        }
        return builder;
    }

    /**
     * Configures default based on annotations.
     *
     * @param builder parameter definition builder
     * @return configured builder
     */
    protected McpToolParameterBuilder configureDefault(McpToolParameterBuilder builder) {
        if (builder.location() == null) {
            if (builder.isSimpleType()) {
                // as RequestParam
                return builder.location(Location.QUERY);
            } else if (builder.isEntityType()) {
                // as ModelAttribute
                return configureModelAttribute(builder, null, Location.QUERY);
            }
        }
        return builder;
    }

    /**
     * Configures @PathVariable based on annotations.
     *
     * @param builder parameter definition builder
     * @param getter  annotation accessor
     * @return configured builder
     */
    protected McpToolParameterBuilder configurePathVariable(McpToolParameterBuilder builder, AnnotationGetter getter) {
        PathVariable pathVariable = getter.getAnnotation(PathVariable.class);
        if (pathVariable != null) {
            String arg = resolveName(choose(pathVariable.value(), pathVariable.name()));
            return builder.arg(arg).required(pathVariable.required()).location(Location.PATH);
        }
        return builder;
    }

    /**
     * Configures @RequestHeader based on annotations.
     *
     * @param builder parameter definition builder
     * @param getter  annotation accessor
     * @return configured builder
     */
    protected McpToolParameterBuilder configureRequestHeader(McpToolParameterBuilder builder, AnnotationGetter getter) {
        if (builder.isType(HttpHeaders.class)) {
            return builder.location(Location.SYSTEM).parser(createMultiValueHeaderParser());
        }
        RequestHeader requestHeader = getter.getAnnotation(RequestHeader.class);
        if (requestHeader != null) {
            if (builder.isAssignableTo(MultiValueMap.class)) {
                return builder.location(Location.SYSTEM).convertable(false).parser(createMultiValueHeaderParser());
            } else if (builder.isAssignableTo(Map.class)) {
                return builder.location(Location.SYSTEM).convertable(false).parser(createSingleValueHeaderParser());
            }
            String arg = resolveName(choose(requestHeader.value(), requestHeader.name()));
            return builder
                    .arg(arg)
                    .location(Location.HEADER)
                    .defaultValueParser(createDefaultValueParser(requestHeader.defaultValue()));
        }
        return builder;
    }

    /**
     * Configures @CookieValue based on annotations.
     *
     * @param builder parameter definition builder
     * @param getter  annotation accessor
     * @return configured builder
     */
    private McpToolParameterBuilder configureCookieValue(McpToolParameterBuilder builder, AnnotationGetter getter) {
        CookieValue cookieValue = getter.getAnnotation(CookieValue.class);
        if (cookieValue != null) {
            String arg = resolveName(choose(cookieValue.value(), cookieValue.name()));
            if (builder.type() == HttpCookie.class) {
                builder.converter(o -> new HttpCookie(builder.key(), o.toString()));
            }
            return builder
                    .arg(arg)
                    .location(Location.COOKIE)
                    .defaultValueParser(createDefaultValueParser(cookieValue.defaultValue()));
        }
        return builder;
    }

    /**
     * Configures @RequestBody based on annotations.
     *
     * @param builder parameter definition builder
     * @param getter  annotation accessor
     * @return configured builder
     */
    protected McpToolParameterBuilder configureRequestBody(McpToolParameterBuilder builder, AnnotationGetter getter) {
        RequestBody requestBody = getter.getAnnotation(RequestBody.class);
        if (requestBody != null) {
            return builder.required(requestBody.required()).location(Location.BODY);
        }
        return builder;
    }

    /**
     * Configures @SessionAttribute based on annotations.
     *
     * @param builder parameter definition builder
     * @param getter  annotation accessor
     * @return configured builder
     */
    protected McpToolParameterBuilder configureSessionAttribute(McpToolParameterBuilder builder, AnnotationGetter getter) {
        SessionAttribute sessionAttribute = getter.getAnnotation(SessionAttribute.class);
        if (sessionAttribute != null) {
            String arg = resolveName(choose(sessionAttribute.value(), sessionAttribute.name()));
            String name = choose(arg, builder.name());
            return builder
                    .arg(arg)
                    .location(Location.SYSTEM)
                    .convertable(false)
                    .parser((req, ctx) -> ctx.getSessionAttribute(name));
        }
        return builder;
    }

    /**
     * Configures @RequestAttribute based on annotations.
     *
     * @param builder parameter definition builder
     * @param getter  annotation accessor
     * @return configured builder
     */
    protected McpToolParameterBuilder configureRequestAttribute(McpToolParameterBuilder builder, AnnotationGetter getter) {
        RequestAttribute requestAttribute = getter.getAnnotation(RequestAttribute.class);
        if (requestAttribute != null) {
            String arg = resolveName(choose(requestAttribute.value(), requestAttribute.name()));
            String name = choose(arg, builder.name());
            return builder
                    .arg(arg)
                    .location(Location.SYSTEM)
                    .convertable(false)
                    .parser((req, ctx) -> ctx.getRequestAttribute(name));
        }
        return builder;
    }

    /**
     * Configures parameter type information.
     *
     * @param builder the parameter builder to configure
     * @return builder with type information configured
     */
    protected abstract McpToolParameterBuilder configureWrapper(McpToolParameterBuilder builder);

    /**
     * Configures system-level parameters.
     *
     * @param builder the parameter builder to configure
     * @return builder with system parameters configured
     */
    protected abstract McpToolParameterBuilder configureSystemParam(McpToolParameterBuilder builder);

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
            return (req, ctx) -> expressionFactory.evaluate(expression);
        } else {
            return (req, ctx) -> function.apply(expressionFactory.evaluate(expression));
        }
    }

    /**
     * Configures @ModelAttribute parameters.
     *
     * @param builder  the parameter builder to configure
     * @param arg      the arg name
     * @param location the location of arg
     * @return builder with system parameters configured
     */
    @SuppressWarnings("unchecked")
    protected McpToolParameterBuilder configureModelAttribute(McpToolParameterBuilder builder, String arg, Location location) {
        return builder
                .arg(arg)
                .location(Location.SYSTEM)
                .parser(((req, ctx) -> {
                    if (location == Location.BODY) {
                        Object value = req.getBody(builder.arg());
                        if (!isEmpty(arg)) {
                            value = value instanceof Map ? cascadeAndGet((Map<String, Object>) value, arg, HashMap::new) : null;
                        }
                        return value;
                    } else {
                        return isEmpty(arg) ? req.getQueries() : cascadeAndGet(req.getQueries(), arg, HashMap::new);
                    }
                }));
    }

    /**
     * Configures validator for MCP tool parameter.
     *
     * @param builder parameter definition builder
     * @return configured builder with validator settings
     */
    protected McpToolParameterBuilder configureValidator(McpToolParameterBuilder builder) {
        // TODO mcp tool parameter validator
        return builder;
    }

    /**
     * Extracts actual type from generic type.
     *
     * @param genericType the generic type
     * @return actual type, or original type if not parameterized
     */
    protected Type getActualType(Type genericType) {
        if (genericType instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) genericType;
            Type[] argTypes = parameterizedType.getActualTypeArguments();
            if (argTypes.length > 0) {
                return argTypes[0];
            }
        }
        return null;
    }

    /**
     * Creates a parser that extracts single-value headers from request.
     * Handles various header map implementations and converts multi-value headers
     * to single values by taking the first entry.
     *
     * @return Parser for single-value header extraction
     */
    @SuppressWarnings("unchecked")
    protected McpRequestParser createSingleValueHeaderParser() {
        return (req, ctx) -> {
            Map<String, ?> values = req.getHeaders();
            if (values instanceof HttpHeaders) {
                return ((HttpHeaders) values).toSingleValueMap();
            } else if (values instanceof MultiMap<?, ?>) {
                return ((MultiMap<String, String>) values).toSingleValueMap();
            } else if (values != null) {
                Map<String, String> result = new LinkedHashMap<>();
                values.forEach((key, value) -> {
                    if (value instanceof List<?>) {
                        List<?> list = (List<?>) value;
                        result.put(key, list.isEmpty() ? "" : ((List<?>) value).get(0).toString());
                    } else {
                        result.put(key, value.toString());
                    }
                });
                return result;
            }
            return new LinkedHashMap<>();
        };
    }

    /**
     * Creates a parser that extracts multi-value headers from request.
     * Preserves all header values and normalizes different map implementations
     * to Spring's HttpHeaders format.
     *
     * @return Parser for multi-value header extraction
     */
    @SuppressWarnings("unchecked")
    protected McpRequestParser createMultiValueHeaderParser() {
        return (req, ctx) -> {
            Map<String, ?> values = req.getHeaders();
            if (values instanceof HttpHeaders) {
                return values;
            }
            HttpHeaders headers = new HttpHeaders();
            if (values instanceof MultiMap<?, ?>) {
                headers.putAll((MultiMap) values);
            } else if (values != null) {
                values.forEach((key, value) -> {
                    if (value instanceof List<?>) {
                        ((List<?>) value).forEach(item -> headers.add(key, item.toString()));
                    } else {
                        headers.add(key, value.toString());
                    }
                });
            }
            return headers;
        };
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
     * Resolves a name by evaluating it as an expression if it's not empty
     *
     * @param name The name to resolve
     * @return Resolved name or original name if empty
     */
    protected String resolveName(String name) {
        return name == null || name.isEmpty() ? name : expressionFactory.evaluate(expressionFactory.parse(name)).toString();
    }

    @Getter
    @AllArgsConstructor
    protected static class PathMethod {

        private final String[] paths;

        private final RequestMethod[] methods;

        public Set<String> mergePaths(PathMethod method) {
            return appendPaths(paths, method == null ? null : method.paths);
        }

        public Set<String> mergeMethods(PathMethod method) {
            RequestMethod[] methods = this.methods;
            if (methods == null || methods.length == 0) {
                methods = method.methods;
            }
            if (methods == null || methods.length == 0) {
                return null;
            }
            Set<String> result = new LinkedHashSet<>();
            for (RequestMethod m : methods) {
                result.add(m.toString());
            }
            return result;
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
    }

}
