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

import com.jd.live.agent.core.util.tag.Label;
import com.jd.live.agent.core.util.type.ValuePath;
import com.jd.live.agent.governance.context.bag.Cargo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Defines an interface for RPC (Remote Procedure Call) requests, extending the {@link ServiceRequest} interface.
 * <p>
 * This interface adds specific functionalities for handling RPC requests, such as determining if the request is a
 * heartbeat or registry operation, accessing method arguments, and retrieving attachments.
 * </p>
 *
 * @since 1.0.0
 */
public interface RpcRequest extends ServiceRequest {

    /**
     * Retrieves a specific argument from the RPC request based on its index.
     *
     * @param index The index of the argument to retrieve.
     * @return The argument at the specified index, or {@code null} if the index is out of bounds or arguments are not available.
     */
    default Object getArgument(int index) {
        Object[] arguments = getArguments();
        return arguments == null || index < 0 || index >= arguments.length ? null : arguments[index];
    }

    @Override
    default List<String> getQueries(String key) {
        List<String> result = new ArrayList<>();
        String query = getQuery(key);
        if (query == null) {
            return result;
        } else {
            result.add(query);
            return result;
        }
    }

    @Override
    default String getQuery(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        Object[] arguments = getArguments();
        if (arguments == null || arguments.length == 0) {
            return null;
        }
        Map<String, Object> params = new HashMap<>(arguments.length);
        for (int i = 0; i < arguments.length; i++) {
            params.put("arg" + i, arguments[i]);
        }

        Object value = ValuePath.of(key).get(params);
        return value == null ? null : value.toString();
    }

    @Override
    default List<String> getHeaders(String key) {
        String header = getHeader(key);
        if (header == null) {
            return null;
        } else {
            return Label.parseValue(header);
        }
    }

    @Override
    default String getHeader(String key) {
        Object attachment = getAttachment(key);
        return attachment == null ? null : attachment.toString();
    }

    @Override
    default List<String> getCookies(String key) {
        Cargo cargo = getCargo(key);
        return cargo == null ? null : cargo.getValues();
    }

    @Override
    default String getCookie(String key) {
        Cargo cargo = getCargo(key);
        return cargo == null ? null : cargo.getFirstValue();
    }

    /**
     * Retrieves the arguments passed with the RPC request.
     *
     * @return An array of {@link Object} containing the arguments.
     */
    Object[] getArguments();

    /**
     * Retrieves an attachment by its key.
     *
     * @param key The key of the attachment to retrieve.
     * @return The attachment object, or {@code null} if no attachment exists for the given key.
     */
    Object getAttachment(String key);

    /**
     * Defines an interface for inbound RPC requests.
     * <p>
     * This interface represents RPC requests that are received by a service from a client or another service.
     * </p>
     *
     * @since 1.0.0
     */
    interface RpcInboundRequest extends RpcRequest, InboundRequest {

    }

    /**
     * Defines an interface for outbound RPC requests.
     * <p>
     * This interface represents RPC requests that are sent from a service to another service or component.
     * </p>
     *
     * @author Zhiguo.Chen
     * @since 1.0.0
     */
    interface RpcOutboundRequest extends RpcRequest, OutboundRequest {

        /**
         * Determine if the current request has disabled traffic management.
         *
         * @return {@code true} if the current request is disabled; {@code false} otherwise.
         */
        default boolean isDisabled() {
            return false;
        }

        /**
         * Checks if this object is a generic type.
         *
         * @return true if this object is a generic type, false otherwise.
         */
        default boolean isGeneric() {
            return false;
        }
    }
}
