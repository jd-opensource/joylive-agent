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
package com.jd.live.agent.plugin.transmission.httpclient.v4_0.contidion;

import com.jd.live.agent.core.extension.annotation.ConditionalComposite;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.ConditionalOnMissingClass;
import com.jd.live.agent.governance.annotation.ConditionalOnTransmissionEnabled;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ConditionalOnTransmissionEnabled
@ConditionalOnClass(ConditionalOnHttpClient40TransmissionEnabled.TYPE_HTTP_CLIENT)
@ConditionalOnMissingClass(ConditionalOnHttpClient40TransmissionEnabled.TYPE_CLOSEABLE_HTTP_CLIENT)
@ConditionalComposite
public @interface ConditionalOnHttpClient40TransmissionEnabled {

    String TYPE_HTTP_CLIENT = "org.apache.http.client.HttpClient";

    String TYPE_CLOSEABLE_HTTP_CLIENT = "org.apache.http.impl.client.CloseableHttpClient";
}
