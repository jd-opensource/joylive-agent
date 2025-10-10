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
package com.jd.live.agent.plugin.router.springcloud.v5.cluster.context;

import com.jd.live.agent.governance.policy.service.loadbalance.StickyType;
import com.jd.live.agent.governance.request.ServiceRequest;
import com.jd.live.agent.governance.request.StickyRequest;
import com.jd.live.agent.governance.request.StickySession;
import com.jd.live.agent.governance.request.StickySession.DefaultStickySession;
import com.jd.live.agent.governance.request.StickySessionFactory;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;

@Getter
@AllArgsConstructor
public class ServiceContext implements StickySessionFactory {

    private RequestLifecycle lifecycle;

    private LoadBalancerProperties loadBalancerProperties;

    @Override
    public StickySession getStickySession(ServiceRequest request) {
        LoadBalancerProperties.StickySession session = loadBalancerProperties.getStickySession();
        if (session == null || !session.isAddServiceInstanceCookie()) {
            return new DefaultStickySession(StickyType.NONE);
        }
        String stickyId = null;
        if (request instanceof StickyRequest) {
            stickyId = ((StickyRequest) request).getStickyId();
        }
        return new DefaultStickySession(StickyType.PREFERRED, stickyId);
    }
}
