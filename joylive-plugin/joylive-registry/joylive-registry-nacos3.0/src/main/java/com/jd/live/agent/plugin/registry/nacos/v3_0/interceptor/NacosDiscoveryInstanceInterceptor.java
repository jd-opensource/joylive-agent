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
package com.jd.live.agent.plugin.registry.nacos.v3_0.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import org.springframework.cloud.client.ServiceInstance;

import java.util.Map;

import static com.jd.live.agent.core.Constants.LABEL_STATE;
import static com.jd.live.agent.core.Constants.LABEL_WEIGHT;
import static com.jd.live.agent.governance.instance.Endpoint.*;

/**
 * Nacos Discovery Instance Interceptor.
 *
 * <p>This interceptor supplements registered instance information by enhancing
 * service instance metadata. It extracts Nacos-specific properties like weight
 * and health status, then standardizes them into common metadata labels for
 * consistent instance management across different registry implementations.</p>
 */
public class NacosDiscoveryInstanceInterceptor extends InterceptorAdaptor {

    @Override
    public void onSuccess(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        ServiceInstance instance = mc.getResult();
        Map<String, String> metadata = instance.getMetadata();
        double weight = Double.parseDouble(metadata.get("nacos.weight"));
        boolean healthy = Boolean.parseBoolean(metadata.get("nacos.healthy"));
        metadata.putIfAbsent(LABEL_WEIGHT, String.valueOf((int) (weight * DEFAULT_WEIGHT)));
        metadata.put(LABEL_STATE, healthy ? STATE_HEALTHY : STATE_SUSPEND);
    }
}
