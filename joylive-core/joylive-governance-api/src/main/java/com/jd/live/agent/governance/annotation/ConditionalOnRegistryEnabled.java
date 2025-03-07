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
package com.jd.live.agent.governance.annotation;

import com.jd.live.agent.core.extension.annotation.ConditionalComposite;
import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.governance.config.GovernanceConfig;

import java.lang.annotation.*;

/**
 * Indicates that the annotated component should only be enabled
 * if the property {@link GovernanceConfig#CONFIG_REGISTRY_ENABLED} is set to true.
 * This annotation is intended to be used at the type level.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_REGISTRY_ENABLED)
@ConditionalComposite
public @interface ConditionalOnRegistryEnabled {

}
