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
package com.jd.live.agent.plugin.transmission.springweb.v6.condition;

import com.jd.live.agent.core.extension.annotation.ConditionalComposite;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.governance.annotation.ConditionalOnTransmissionEnabled;

import java.lang.annotation.*;

/**
 * An annotation used to mark a type as requiring specific conditions related to Spring Web to be met.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ConditionalOnTransmissionEnabled
@ConditionalOnClass(ConditionalOnSpringWeb6TransmissionEnabled.TYPE_ERROR_RESPONSE)
@ConditionalOnClass(ConditionalOnSpringWeb6TransmissionEnabled.TYPE_NESTED_SERVLET_EXCEPTION)
@ConditionalComposite
public @interface ConditionalOnSpringWeb6TransmissionEnabled {

    // spring web 6+
    String TYPE_ERROR_RESPONSE = "org.springframework.web.ErrorResponse";

    // spring web 5/6
    String TYPE_NESTED_SERVLET_EXCEPTION = "org.springframework.web.util.NestedServletException";

}
