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
package com.jd.live.agent.plugin.router.springcloud.v4.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.core.util.type.ClassUtils;
import com.jd.live.agent.core.util.type.FieldDesc;
import com.jd.live.agent.governance.context.RequestContext;
import com.jd.live.agent.governance.request.Request;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.core.SameInstancePreferenceServiceInstanceListSupplier;

import java.util.List;

/**
 * PreferSameInstanceInterceptor
 *
 * @since 1.0.0
 */
public class PreferSameInstanceInterceptor extends InterceptorAdaptor {

    public static final String FIELD_PREVIOUSLY_RETURNED_INSTANCE = "previouslyReturnedInstance";
    private FieldDesc fieldDesc;

    public PreferSameInstanceInterceptor() {
    }

    /**
     * Enhanced logic before method execution
     *
     * @param ctx The execution context of the method being intercepted.
     *            <p> see org.springframework.cloud.loadbalancer.core.SameInstancePreferenceServiceInstanceListSupplier#filteredBySameInstancePreference(List)
     */
    @SuppressWarnings("unchecked")
    @Override
    public void onEnter(ExecutableContext ctx) {
        Object[] arguments = ctx.getArguments();
        List<ServiceInstance> instances = (List<ServiceInstance>) arguments[0];
        if (fieldDesc == null) {
            this.fieldDesc = ClassUtils.describe(SameInstancePreferenceServiceInstanceListSupplier.class)
                    .getFieldList().getField(FIELD_PREVIOUSLY_RETURNED_INSTANCE);
        }
        ServiceInstance instance = (ServiceInstance) fieldDesc.get(ctx.getTarget());
        // convert to sticky id
        RequestContext.getOrCreate().setAttribute(Request.KEY_STICKY_ID, instance == null ? null : instance.getInstanceId());
        MethodContext mc = (MethodContext) ctx;
        mc.setResult(instances);
        mc.setSkip(true);
    }
}
