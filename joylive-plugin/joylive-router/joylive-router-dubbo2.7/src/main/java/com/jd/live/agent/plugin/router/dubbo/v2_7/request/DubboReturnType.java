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
package com.jd.live.agent.plugin.router.dubbo.v2_7.request;

import com.jd.live.agent.governance.request.RpcReturnType;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.utils.ReflectUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.service.GenericService;
import org.apache.dubbo.rpc.support.ProtocolUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

import static org.apache.dubbo.common.constants.CommonConstants.$INVOKE;
import static org.apache.dubbo.common.constants.CommonConstants.$INVOKE_ASYNC;

/**
 * Dubbo-specific implementation of RPC return type.
 */
public class DubboReturnType extends RpcReturnType {

    public DubboReturnType(ClassLoader classLoader, Class<?> returnClass, Type returnType, String generic) {
        super(classLoader, returnClass, returnType, generic);
    }

    /**
     * Creates a DubboReturnType from the given invocation.
     *
     * @param invocation the RPC invocation
     * @return the DubboReturnType instance
     * @throws Exception if type resolution fails
     */
    public static DubboReturnType of(Invocation invocation) throws Exception {
        URL url = invocation.getInvoker().getUrl();
        String generic = url.getParameter(CommonConstants.GENERIC_KEY);
        Type[] result = null;
        String methodName = invocation.getMethodName();
        if (ProtocolUtils.isGeneric(generic)) {
            result = $INVOKE.equals(methodName) || $INVOKE_ASYNC.equals(methodName)
                    ? new Type[]{null, null}
                    : getReturnTypes(invocation, url, true, methodName);
        } else {
            if (invocation instanceof RpcInvocation) {
                result = ((RpcInvocation) invocation).getReturnTypes();
            }
            if (result == null) {
                result = getReturnTypes(invocation, url, false, methodName);
            }
        }
        return new DubboReturnType(url.getClass().getClassLoader(), (Class<?>) result[0], result[1], generic);
    }

    /**
     * Gets the return types for the specified method in the invocation.
     *
     * @param invocation the RPC invocation
     * @param url        the service URL
     * @param generic    whether this is a generic service call
     * @param methodName the target method name
     * @return array containing return type and generic return type
     * @throws Exception if method reflection fails
     */
    private static Type[] getReturnTypes(Invocation invocation, URL url, boolean generic, String methodName) throws Exception {
        Class<?> type = getInterface(invocation, url, generic);
        Method method = type.getMethod(methodName, invocation.getParameterTypes());
        return ReflectUtils.getReturnTypes(method);
    }

    /**
     * Gets the interface class for the invocation.
     *
     * @param invocation the RPC invocation
     * @param url        the service URL
     * @param generic    whether this is a generic service call
     * @return the interface class, real interface for generic services
     */
    private static Class<?> getInterface(Invocation invocation, URL url, boolean generic) {
        Class<?> result = invocation.getInvoker().getInterface();
        if (generic && result.isAssignableFrom(GenericService.class)) {
            try {
                // find the real interface from url
                result = ReflectUtils.forName(invocation.getClass().getClassLoader(), url.getServiceInterface());
            } catch (Throwable e) {
                // ignore
            }
        }
        return result;
    }
}
