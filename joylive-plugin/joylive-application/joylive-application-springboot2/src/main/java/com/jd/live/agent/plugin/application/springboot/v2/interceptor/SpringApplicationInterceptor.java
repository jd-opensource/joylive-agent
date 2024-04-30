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
package com.jd.live.agent.plugin.application.springboot.v2.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.core.event.AgentEvent;
import com.jd.live.agent.core.event.AgentEvent.EventType;
import com.jd.live.agent.core.event.Event;
import com.jd.live.agent.core.event.Publisher;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.policy.PolicySubscriber;
import com.jd.live.agent.governance.policy.PolicySupervisor;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public class SpringApplicationInterceptor extends InterceptorAdaptor {

    private final PolicySupervisor policySupervisor;

    private final GovernanceConfig config;

    private final Publisher<AgentEvent> publisher;

    public SpringApplicationInterceptor(PolicySupervisor policySupervisor, GovernanceConfig config, Publisher<AgentEvent> publisher) {
        this.policySupervisor = policySupervisor;
        this.config = config;
        this.publisher = publisher;
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        List<PolicySubscriber> subscribers = policySupervisor.getSubscribers();
        if (subscribers != null && !subscribers.isEmpty()) {
            List<CompletableFuture<Void>> futures = subscribers.stream().map(PolicySubscriber::getFuture).collect(Collectors.toList());
            try {
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(config.getInitializeTimeout(), TimeUnit.MILLISECONDS);
                publisher.offer(new Event<>(new AgentEvent(EventType.AGENT_POLICY_INITIALIZE_SUCCESS, "success fetching all policies.")));
            } catch (InterruptedException ignore) {
            } catch (Throwable e) {
                String error = e instanceof TimeoutException ? "it's timeout to fetch governance policy." :
                        "failed to fetch governance policy. caused by " + e.getMessage();
                publisher.offer(new Event<>(new AgentEvent(EventType.AGENT_POLICY_INITIALIZE_FAILURE, error)));
            }
        }
    }
}
