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
package com.jd.live.agent.plugin.registry.sofarpc.interceptor;

import com.alipay.sofa.rpc.bootstrap.ConsumerBootstrap;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.governance.registry.Registry;
import com.jd.live.agent.governance.registry.ServiceId;

/**
 * ConsumerBootstrapInterceptor
 */
public class ConsumerBootstrapInterceptor extends AbstractBootstrapInterceptor<ConsumerConfig<?>> {

    private static final Logger logger = LoggerFactory.getLogger(ConsumerBootstrapInterceptor.class);

    public ConsumerBootstrapInterceptor(Application application, Registry registry) {
        super(application, registry);
    }

    @Override
    protected void subscribe(ConsumerConfig<?> config) {
        ServiceId serviceId = new ServiceId(config.getInterfaceId(), getGroup(config), true);
        registry.subscribe(serviceId);
        logger.info("Found sofa rpc consumer {}.", serviceId.getUniqueName());
    }

    @Override
    protected ConsumerConfig<?> getConfig(ExecutableContext ctx) {
        return ((ConsumerBootstrap<?>) ctx.getTarget()).getConsumerConfig();
    }
}
