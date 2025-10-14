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
package com.jd.live.agent.plugin.router.sofarpc.request;

import com.alipay.hessian.generic.model.GenericObject;
import com.alipay.sofa.rpc.api.GenericContext;
import com.alipay.sofa.rpc.common.RemotingConstants;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.jd.live.agent.governance.request.RpcReturnType;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.function.Function;

/**
 * SOFA-specific implementation of RPC return type.
 */
public class SofaReturnType extends RpcReturnType {

    /**
     * generic call
     */
    private static final String METHOD_$INVOKE = "$invoke";

    private static final String METHOD_$GENERIC_INVOKE = "$genericInvoke";

    public SofaReturnType(ClassLoader classLoader, Class<?> returnClass, Type returnType) {
        super(classLoader, returnClass, returnType);
    }

    public SofaReturnType(ClassLoader classLoader, Class<?> returnClass, String generic) {
        super(classLoader, returnClass, returnClass, generic);
    }

    public SofaReturnType(ClassLoader classLoader, Class<?> returnClass, String generic, Function<Object, Object> converter) {
        super(classLoader, returnClass, returnClass, generic, converter);
    }

    /**
     * Creates a SofaReturnType from the given SOFA request.
     *
     * @param request the SOFA request
     * @return the SofaReturnType instance
     */
    public static SofaReturnType of(SofaRequest request) {
        String methodName = request == null ? null : request.getMethodName();
        ClassLoader classLoader = request.getClass().getClassLoader();
        if (METHOD_$INVOKE.equals(methodName)) {
            return new SofaReturnType(classLoader, null, RemotingConstants.SERIALIZE_FACTORY_NORMAL);
        } else if (METHOD_$GENERIC_INVOKE.equals(methodName)) {
            Object[] args = request.getMethodArgs();
            if (request.getTimeout() == null) {
                request.setTimeout(getTimeout(args));
            }
            if (args.length == 4 && args[3] instanceof Class || args.length == 5) {
                return new SofaReturnType(classLoader, (Class<?>) args[3], RemotingConstants.SERIALIZE_FACTORY_MIX);
            }
            return new SofaReturnType(classLoader, Object.class, RemotingConstants.SERIALIZE_FACTORY_GENERIC, SofaReturnType::convertGenericObject);
        }
        Method method = request.getMethod();
        return new SofaReturnType(classLoader, method.getReturnType(), method.getGenericReturnType());
    }

    /**
     * Extracts timeout value from method arguments.
     *
     * @param args method arguments array
     * @return timeout value in milliseconds, or null if not found
     */
    private static Integer getTimeout(Object[] args) {
        if (args.length == 4 && args[3] instanceof GenericContext) {
            return (int) ((GenericContext) args[3]).getClientTimeout();
        } else if (args.length == 5 && args[4] instanceof GenericContext) {
            return (int) ((GenericContext) args[4]).getClientTimeout();
        }
        return null;
    }

    /**
     * Converts a value to generic object representation.
     *
     * @param value the value to convert
     * @return the converted generic object
     */
    private static Object convertGenericObject(Object value) {
        if (value instanceof Map) {
            GenericObject object = new GenericObject(value.getClass().getName());
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                object.putField(entry.getKey().toString(), convertGenericObject(entry.getValue()));
            }
            return object;
        } else {
            return value;
        }
    }
}
