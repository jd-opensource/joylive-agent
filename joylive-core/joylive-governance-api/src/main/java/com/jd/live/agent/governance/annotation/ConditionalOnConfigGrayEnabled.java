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
 * Conditional annotation that enables configuration gray release functionality.
 * <p>
 * This annotation ensures that configuration gray release is enabled by checking:
 * <ul>
 *   <li>Flow control is enabled</li>
 *   <li>Config center gray release property is set to true</li>
 * </ul>
 * Apply this annotation to components that should only be active when gray configuration is enabled.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ConditionalOnFlowControlEnabled
@ConditionalOnProperty(name = GovernanceConfig.CONFIG_CENTER_ENABLED, value = "false")
@ConditionalOnProperty(GovernanceConfig.CONFIG_CENTER_GRAY_ENABLED)
@ConditionalComposite
public @interface ConditionalOnConfigGrayEnabled {

}
