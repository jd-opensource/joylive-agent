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
package com.jd.live.agent.plugin.router.springcloud.v2_2.definition;

import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.governance.annotation.ConditionalOnFlowControlEnabled;
import com.jd.live.agent.plugin.router.springcloud.v2_2.condition.ConditionalOnSpringWeb5Enabled;
import com.jd.live.agent.plugin.router.springcloud.v2_2.interceptor.FeignRetryerInterceptor;

/**
 * FeignRetryerDefinition
 */
@Extension(value = "FeignRetryerDefinition_v5")
@ConditionalOnFlowControlEnabled
@ConditionalOnSpringWeb5Enabled
@ConditionalOnClass(FeignRetryerDefinition.TYPE)
public class FeignRetryerDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE = "feign.SynchronousMethodHandler$Factory";

    public FeignRetryerDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(MatcherBuilder.isConstructor(), () -> new FeignRetryerInterceptor())
        };
    }
}
