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
package com.jd.live.agent.governance.bootstrap;

import com.jd.live.agent.core.bootstrap.ApplicationContext;
import com.jd.live.agent.core.bootstrap.ApplicationListener;
import com.jd.live.agent.core.bootstrap.ApplicationListener.ApplicationListenerAdapter;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.governance.policy.PolicySupervisor;

/**
 * An extension that prepares policies for the application.
 *
 * @since 1.6.0
 */
@Injectable
@Extension(value = "PolicyPreparation", order = ApplicationListener.ORDER_POLICY_PREPARATION)
public class PolicyPreparation extends ApplicationListenerAdapter {

    @Inject(PolicySupervisor.COMPONENT_POLICY_SUPERVISOR)
    private PolicySupervisor policySupervisor;

    @Override
    public void onStarted(ApplicationContext context) {
        policySupervisor.waitReady();
    }
}
