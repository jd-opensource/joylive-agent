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
package com.jd.live.agent.governance.policy.listener;

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.config.ConfigListener;
import com.jd.live.agent.core.config.Configuration;
import com.jd.live.agent.governance.policy.GovernancePolicy;
import com.jd.live.agent.governance.policy.PolicySupervisor;

public abstract class AbstractListener<T> implements ConfigListener {

    private static final Logger logger = LoggerFactory.getLogger(AbstractListener.class);

    protected final PolicySupervisor supervisor;

    public AbstractListener(PolicySupervisor supervisor) {
        this.supervisor = supervisor;
    }

    @Override
    public void onUpdate(Configuration config) {
        if (config != null) {
            try {
                update(config);
            } catch (Throwable e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    protected void update(Configuration config) {
        if (supervisor.update(policy -> newPolicy(policy, config))) {
            String action = config.getValue() == null || config.getValue().isEmpty() ? "deleting" : "updating";
            logger.info("Success " + action + " " + config.getDescription());
            onSuccess(config);
        }
    }

    protected GovernancePolicy newPolicy(GovernancePolicy policy, Configuration config) {
        GovernancePolicy update = policy == null ? new GovernancePolicy() : policy.copy();
        update(policy, parse(config), config.getWatcher());
        return update;
    }

    protected abstract T parse(Configuration config);

    protected abstract void update(GovernancePolicy policy, T value, String watcher);

    protected void onSuccess(Configuration config) {

    }

}
