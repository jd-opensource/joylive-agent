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
package com.jd.live.agent.plugin.transmission.httpclient.v4.definition;

import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinition;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.governance.context.bag.Propagation;
import com.jd.live.agent.plugin.transmission.httpclient.v4.contidion.ConditionalOnHttpClient4TransmissionEnabled;
import com.jd.live.agent.plugin.transmission.httpclient.v4.interceptor.HttpClientInterceptor;

@Injectable
@Extension(value = "HttpClientDefinition_v4", order = PluginDefinition.ORDER_TRANSMISSION)
@ConditionalOnHttpClient4TransmissionEnabled
public class HttpClientDefinition extends PluginDefinitionAdapter {

    private static final String[] TYPES = {
            // 4.0+
            "org.apache.http.impl.client.AbstractHttpClient",
            // 4.3+
            "org.apache.http.impl.client.InternalHttpClient",
            // 4.3+
            "org.apache.http.impl.client.MinimalHttpClient",
            // 4.3+
            "org.apache.http.impl.client.DefaultRequestDirector"
    };

    private static final String[] METHODS = {"execute", "doExecute"};

    private static final String[] ARGUMENTS = new String[]{
            "org.apache.http.HttpHost",
            "org.apache.http.HttpRequest",
            "org.apache.http.protocol.HttpContext"
    };

    @Inject(value = Propagation.COMPONENT_PROPAGATION, component = true)
    private Propagation propagation;

    public HttpClientDefinition() {
        this.matcher = () -> MatcherBuilder.in(TYPES);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.in(METHODS).and(MatcherBuilder.arguments(ARGUMENTS)),
                        () -> new HttpClientInterceptor(propagation))};
    }
}
