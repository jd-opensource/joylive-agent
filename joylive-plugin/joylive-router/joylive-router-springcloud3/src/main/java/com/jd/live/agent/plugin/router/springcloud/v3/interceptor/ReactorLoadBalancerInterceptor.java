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
package com.jd.live.agent.plugin.router.springcloud.v3.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.core.util.type.ClassUtils;
import com.jd.live.agent.core.util.type.FieldDesc;
import com.jd.live.agent.governance.context.RequestContext;
import com.jd.live.agent.governance.context.bag.Carrier;

/**
 * ReactorLoadBalancerInterceptor
 *
 * @since 1.0.0
 */
public class ReactorLoadBalancerInterceptor extends InterceptorAdaptor {

    private static final String FIELD_SERVICE_ID = "serviceId";

    @Override
    public void onEnter(ExecutableContext ctx) {
        Object target = ctx.getTarget();
        FieldDesc fieldDesc = ClassUtils.describe(target.getClass()).getFieldList().getField(FIELD_SERVICE_ID);
        if (fieldDesc != null) {
            try {
                String serviceId = (String) fieldDesc.get(target);
                if (serviceId != null) {
                    RequestContext.getOrCreate().setAttribute(Carrier.ATTRIBUTE_SERVICE_ID, serviceId);
                }
            } catch (Throwable ignore) {
            }
        }
    }
}
