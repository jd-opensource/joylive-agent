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
package com.jd.live.agent.plugin.router.gprc.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.core.util.type.ClassDesc;
import com.jd.live.agent.core.util.type.ClassUtils;
import com.jd.live.agent.core.util.type.FieldDesc;
import com.jd.live.agent.plugin.router.gprc.loadbalance.LiveDiscovery;
import net.devh.boot.grpc.client.inject.GrpcClient;

import java.lang.reflect.Field;
import java.lang.reflect.Member;

/**
 * GrpcClientBeanInterceptor
 */
public class GrpcClientBeanInterceptor extends InterceptorAdaptor {

    private static final String FIELD_SERVICE_NAME = "SERVICE_NAME";

    @Override
    public void onEnter(ExecutableContext ctx) {
        Member member = ctx.getArgument(0);
        Class<?> type = ctx.getArgument(1);
        GrpcClient grpcClient = ctx.getArgument(2);
        if (member instanceof Field) {
            String service = grpcClient.value();
            ClassDesc classDesc = ClassUtils.describe(type.getDeclaringClass());
            FieldDesc fieldDesc = classDesc.getFieldList().getField(FIELD_SERVICE_NAME);
            String interfaceName = (String) fieldDesc.get(null);
            LiveDiscovery.putService(interfaceName, service);
        }
    }

}
