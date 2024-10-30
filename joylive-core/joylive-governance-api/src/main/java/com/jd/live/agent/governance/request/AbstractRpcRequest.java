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
package com.jd.live.agent.governance.request;

import java.util.Map;

/**
 * Provides an abstract base class for RPC (Remote Procedure Call) requests, implementing the {@link RpcRequest} interface.
 * <p>
 * This class encapsulates common properties and behaviors for RPC requests, such as service identification, method invocation details,
 * and handling of arguments and attachments.
 * </p>
 *
 * @param <T> The type of the original request object this class wraps.
 */
public abstract class AbstractRpcRequest<T> extends AbstractServiceRequest<T> implements RpcRequest {

    /**
     * The name of the service being requested. This typically corresponds to a specific functionality or group of functionalities.
     */
    protected String service;

    /**
     * The group or category that the service belongs to. This can be used to organize services into logical groupings.
     */
    protected String group;

    /**
     * The path of the service request. This may be used to further specify the target within the service, or it could be null if not applicable.
     */
    protected String path;

    /**
     * The name of the method being invoked on the service. This specifies the action to be performed by the service.
     */
    protected String method;

    /**
     * The arguments to be passed to the method being invoked. This is an array of objects that represent the parameters of the method call.
     */
    protected Object[] arguments;

    /**
     * A collection of additional data or metadata associated with the request. This map allows for flexible attachment of key-value pairs.
     */
    protected Map<String, ?> attachments;


    /**
     * Constructs an instance of {@code AbstractRpcRequest} with specified details.
     *
     * @param request     The original request object.
     * @param service     The name of the service being requested.
     * @param group       The group of the service.
     * @param path        The path of the service request.
     * @param method      The method being invoked on the service.
     * @param arguments   The arguments to the method call.
     * @param attachments Additional attachments or metadata for the request.
     */
    public AbstractRpcRequest(T request, String service, String group, String path, String method,
                              Object[] arguments, Map<String, ?> attachments) {
        super(request);
        this.service = service;
        this.group = group;
        this.method = method;
        // normalize path in service meta data parse.
        this.path = path;
        this.arguments = arguments;
        this.attachments = attachments;
    }

    /**
     * Constructs an instance of {@code AbstractRpcRequest} with the original request object.
     *
     * @param request The original request object.
     */
    public AbstractRpcRequest(T request) {
        super(request);
    }

    @Override
    public String getService() {
        return service;
    }

    @Override
    public String getGroup() {
        return group;
    }

    @Override
    public boolean isNativeGroup() {
        return true;
    }

    @Override
    public String getMethod() {
        return method;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public Object[] getArguments() {
        return arguments;
    }

    @Override
    public Object getAttachment(String key) {
        return key == null || attachments == null ? null : attachments.get(key);
    }

    /**
     * Loads a class with the given name, using the class loader of the request class.
     * If the class cannot be found, returns the default class instead.
     *
     * @param className The name of the class to load.
     * @param def       The default class to return if the requested class cannot be found.
     * @return The loaded class, or the default class if the requested class cannot be found.
     */
    public Class<?> loadClass(String className, Class<?> def) {
        if (className != null && !className.isEmpty()) {
            try {
                return request.getClass().getClassLoader().loadClass(className);
            } catch (ClassNotFoundException ignored) {
            }
        }
        return def;
    }

    /**
     * Provides an abstract base class for inbound RPC requests.
     * <p>
     * This class represents RPC requests that are received by a service from a client or another service.
     * </p>
     *
     * @param <T> The type of the original request object.
     */
    public abstract static class AbstractRpcInboundRequest<T> extends AbstractRpcRequest<T>
            implements RpcInboundRequest {

        public AbstractRpcInboundRequest(T request, String service, String group, String path, String method,
                                         Object[] arguments, Map<String, ?> attachments) {
            super(request, service, group, path, method, arguments, attachments);
        }

        public AbstractRpcInboundRequest(T request) {
            super(request);
        }
    }

    /**
     * Provides an abstract base class for outbound RPC requests.
     * <p>
     * This class represents RPC requests that are sent from a service to another service or component.
     * </p>
     *
     * @param <T> The type of the original request object.
     */
    public abstract static class AbstractRpcOutboundRequest<T> extends AbstractRpcRequest<T>
            implements RpcOutboundRequest {

        public AbstractRpcOutboundRequest(T request, String service, String group, String path, String method,
                                          Object[] arguments, Map<String, ?> attachments) {
            super(request, service, group, path, method, arguments, attachments);
        }

        public AbstractRpcOutboundRequest(T request) {
            super(request);
        }
    }
}
